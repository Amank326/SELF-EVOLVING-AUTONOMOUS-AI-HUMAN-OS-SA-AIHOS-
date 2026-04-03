package com.aihos.ui.render.audio
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
/**
 * AudioResonanceAnalyzer - Real-time FFT-based audio analysis engine.
 *
 * Architecture:
 *   AudioRecord (mic) -> ring buffer -> FFT -> band extraction -> snapshot
 *
 * Runs on its own dedicated thread. Produces AudioResonanceSnapshot
 * and pushes to AudioResonanceBridge (lock-free).
 *
 * FFT window: 1024 samples at 44100 Hz = ~23ms latency
 * Frequency resolution: 44100/1024 ~= 43 Hz per bin
 *
 * Band mapping:
 *   Bass:  bins 1-5   (43-215 Hz)
 *   Mid:   bins 6-93  (258-4000 Hz)
 *   High:  bins 94-512 (4043-22050 Hz)
 */
class AudioResonanceAnalyzer(
    private val bridge: AudioResonanceBridge
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FFT_SIZE = 1024
        private const val HALF_FFT = FFT_SIZE / 2
        // Band boundaries in FFT bins
        private const val BASS_START = 1
        private const val BASS_END = 5      // ~43-215 Hz
        private const val MID_START = 6
        private const val MID_END = 93      // ~258-4000 Hz
        private const val HIGH_START = 94
        private const val HIGH_END = HALF_FFT // ~4043-22050 Hz
        // Smoothing factors (exponential moving average)
        private const val SMOOTH_FAST = 0.3f  // responsive
        private const val SMOOTH_SLOW = 0.08f // stable
    }
    @Volatile private var isRunning = false
    private var analyzerThread: Thread? = null
    // Pre-allocated buffers (zero allocation in analysis loop)
    private val audioBuffer = ShortArray(FFT_SIZE)
    private val fftReal = FloatArray(FFT_SIZE)
    private val fftImag = FloatArray(FFT_SIZE)
    private val magnitudes = FloatArray(HALF_FFT)
    private val prevMagnitudes = FloatArray(HALF_FFT)
    private val window = FloatArray(FFT_SIZE)
    // Smoothed outputs
    private var smoothBass = 0f
    private var smoothMid = 0f
    private var smoothHigh = 0f
    private var smoothAmplitude = 0f
    private var smoothFlux = 0f
    private var prevFlux = 0f
    // Onset detection
    private var onsetThreshold = 0.15f
    private var onsetCooldown = 0
    init {
        // Pre-compute Hann window
        for (i in 0 until FFT_SIZE) {
            window[i] = (0.5 * (1.0 - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
        }
    }
    /**
     * Start audio capture and analysis on a dedicated thread.
     * Requires RECORD_AUDIO permission.
     */
    fun start(): Boolean {
        if (isRunning) return true
        val bufSize = max(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            FFT_SIZE * 2
        )
        val recorder: AudioRecord
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Timber.w("AudioResonanceAnalyzer: AudioRecord failed to initialize")
                recorder.release()
                return false
            }
        } catch (e: SecurityException) {
            Timber.w("AudioResonanceAnalyzer: RECORD_AUDIO permission not granted")
            return false
        } catch (e: Exception) {
            Timber.e(e, "AudioResonanceAnalyzer: failed to create AudioRecord")
            return false
        }
        isRunning = true
        analyzerThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            recorder.startRecording()
            Timber.i("AudioResonanceAnalyzer: started (FFT=FFT_SIZE, rate=SAMPLE_RATE)")
            try {
                while (isRunning) {
                    val read = recorder.read(audioBuffer, 0, FFT_SIZE)
                    if (read == FFT_SIZE) {
                        processFrame()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "AudioResonanceAnalyzer: error in analysis loop")
            } finally {
                try {
                    recorder.stop()
                    recorder.release()
                } catch (e: Exception) { /* ignore */ }
                Timber.i("AudioResonanceAnalyzer: stopped")
            }
        }, "AudioResonanceAnalyzer")
        analyzerThread?.isDaemon = true
        analyzerThread?.start()
        return true
    }
    fun stop() {
        isRunning = false
        analyzerThread?.join(500)
        analyzerThread = null
        bridge.clear()
    }
    // ===============================================================
    // Core DSP Pipeline (zero allocation)
    // ===============================================================
    private fun processFrame() {
        // 1. Convert PCM16 to float + apply window
        for (i in 0 until FFT_SIZE) {
            fftReal[i] = (audioBuffer[i].toFloat() / 32768f) * window[i]
            fftImag[i] = 0f
        }
        // 2. In-place FFT (Cooley-Tukey radix-2)
        fft(fftReal, fftImag, FFT_SIZE)
        // 3. Compute magnitudes + save previous for spectral flux
        System.arraycopy(magnitudes, 0, prevMagnitudes, 0, HALF_FFT)
        for (i in 0 until HALF_FFT) {
            val re = fftReal[i]
            val im = fftImag[i]
            magnitudes[i] = sqrt(re * re + im * im)
        }
        // 4. Extract band energies
        val rawBass = bandEnergy(BASS_START, BASS_END)
        val rawMid = bandEnergy(MID_START, MID_END)
        val rawHigh = bandEnergy(HIGH_START, HIGH_END)
        // 5. RMS amplitude
        var sumSq = 0f
        for (i in 0 until FFT_SIZE) {
            val s = audioBuffer[i].toFloat() / 32768f
            sumSq += s * s
        }
        val rawAmplitude = sqrt(sumSq / FFT_SIZE)
        // 6. Spectral flux (positive half-wave rectified)
        var flux = 0f
        for (i in 0 until HALF_FFT) {
            val diff = magnitudes[i] - prevMagnitudes[i]
            if (diff > 0f) flux += diff
        }
        flux = min(flux / HALF_FFT * 10f, 1f)
        // 7. Normalize bands using log scaling
        val normBass = logNormalize(rawBass, 0.001f, 0.5f)
        val normMid = logNormalize(rawMid, 0.0005f, 0.15f)
        val normHigh = logNormalize(rawHigh, 0.0002f, 0.05f)
        val normAmp = min(rawAmplitude * 5f, 1f)
        // 8. Exponential smoothing
        smoothBass = lerp(smoothBass, normBass, SMOOTH_FAST)
        smoothMid = lerp(smoothMid, normMid, SMOOTH_FAST)
        smoothHigh = lerp(smoothHigh, normHigh, SMOOTH_FAST)
        smoothAmplitude = lerp(smoothAmplitude, normAmp, SMOOTH_FAST)
        smoothFlux = lerp(smoothFlux, flux, SMOOTH_SLOW)
        // 9. Onset detection
        val isOnset: Boolean
        if (onsetCooldown > 0) {
            onsetCooldown--
            isOnset = false
        } else {
            isOnset = flux > onsetThreshold && flux > prevFlux * 1.5f
            if (isOnset) onsetCooldown = 4
        }
        prevFlux = flux
        // 10. Push to bridge
        bridge.pushState(AudioResonanceSnapshot(
            bassEnergy = smoothBass,
            midEnergy = smoothMid,
            highEnergy = smoothHigh,
            amplitude = smoothAmplitude,
            onsetDetected = isOnset,
            spectralFlux = smoothFlux,
            isActive = true
        ))
    }
    private fun bandEnergy(startBin: Int, endBin: Int): Float {
        var sum = 0f
        val count = endBin - startBin + 1
        for (i in startBin..min(endBin, HALF_FFT - 1)) {
            sum += magnitudes[i]
        }
        return sum / count
    }
    private fun logNormalize(value: Float, minVal: Float, maxVal: Float): Float {
        if (value <= minVal) return 0f
        if (value >= maxVal) return 1f
        val logMin = ln(minVal)
        val logMax = ln(maxVal)
        val logVal = ln(value)
        return ((logVal - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
    }
    private fun lerp(current: Float, target: Float, alpha: Float): Float {
        return current + (target - current) * alpha
    }
    // ===============================================================
    // Cooley-Tukey FFT (in-place, radix-2, zero allocation)
    // ===============================================================
    private fun fft(real: FloatArray, imag: FloatArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
            var k = n / 2
            while (k <= j) { j -= k; k /= 2 }
            j += k
        }
        // Butterfly computation
        var step = 1
        while (step < n) {
            val halfStep = step
            step *= 2
            val wAngle = -PI / halfStep
            for (group in 0 until n step step) {
                for (pair in 0 until halfStep) {
                    val angle = wAngle * pair
                    val wRe = cos(angle).toFloat()
                    val wIm = kotlin.math.sin(angle).toFloat()
                    val a = group + pair
                    val b = a + halfStep
                    val tRe = wRe * real[b] - wIm * imag[b]
                    val tIm = wRe * imag[b] + wIm * real[b]
                    real[b] = real[a] - tRe
                    imag[b] = imag[a] - tIm
                    real[a] = real[a] + tRe
                    imag[a] = imag[a] + tIm
                }
            }
        }
    }
}
