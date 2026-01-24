package com.aihos.ai.ml.impl

import android.content.Context
import com.aihos.ai.ml.MLModelType
import com.aihos.ai.ml.ModelMetadata
import com.aihos.ai.ml.TFLiteInterpreterProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite Interpreter Provider Implementation
 * Encapsulates single model's interpreter, tensor handling, and execution
 *
 * Responsibilities:
 * - Load model from assets or disk
 * - Initialize TFLite interpreter with optional NNAPI delegate
 * - Handle input/output tensor setup
 * - Execute inference synchronously
 * - Proper cleanup to prevent memory leaks
 *
 * Thread Safety:
 * - All operations must be synchronized externally (via Mutex in manager)
 * - Interpreter is NOT thread-safe, only one inference at a time
 */
class TFLiteInterpreterProviderImpl(
    private val context: Context,
    override val modelMetadata: ModelMetadata,
    private val useNNAPI: Boolean = true  // Try to use hardware acceleration
) : TFLiteInterpreterProvider {
    
    private var interpreter: Interpreter? = null
    private var inputBuffer: FloatArray? = null
    private var outputBuffer: FloatArray? = null
    
    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                Timber.d("Initializing TFLite interpreter for ${modelMetadata.type}")
                
                // Load model from assets
                val buffer = loadModelAsset()
                if (buffer == null) {
                    Timber.e("Failed to load model asset: ${modelMetadata.filePath}")
                    return@withContext false
                }
                
                // Create interpreter options
                val options = Interpreter.Options()
                
                // Try to use NNAPI for hardware acceleration (GPU/NPU)
                if (useNNAPI) {
                    try {
                        val compatibilityList = CompatibilityList()
                        if (compatibilityList.isDelegateSupportedOnThisDevice) {
                            Timber.d("NNAPI delegate available, enabling for ${modelMetadata.type}")
                            options.addDelegate(compatibilityList.gpuDelegate)
                        } else {
                            Timber.w("NNAPI not available on this device")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error initializing NNAPI delegate, continuing without")
                    }
                }
                
                // Set number of threads
                options.setNumThreads(2)
                
                // Create interpreter
                interpreter = Interpreter(buffer, options)
                
                // Allocate tensor buffers
                inputBuffer = FloatArray(modelMetadata.inputShape.fold(1) { acc, dim -> acc * dim })
                outputBuffer = FloatArray(modelMetadata.outputSize)
                
                Timber.i("Successfully initialized ${modelMetadata.type} interpreter")
                Timber.d("  Input size: ${inputBuffer?.size}")
                Timber.d("  Output size: ${outputBuffer?.size}")
                
                return@withContext true
                
            } catch (e: Exception) {
                Timber.e(e, "Fatal error initializing interpreter for ${modelMetadata.type}")
                cleanup()
                return@withContext false
            }
        }
    }
    
    /**
     * Load model from assets
     */
    private fun loadModelAsset(): MappedByteBuffer? {
        return try {
            val modelFile = File(context.cacheDir, modelMetadata.filePath)
            
            // If model doesn't exist in cache, try loading from assets
            if (!modelFile.exists()) {
                Timber.d("Model not in cache, attempting to load from assets: ${modelMetadata.filePath}")
                val assetManager = context.assets
                assetManager.open(modelMetadata.filePath).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Model copied to cache")
            }
            
            // Memory-map the file
            val channel = FileInputStream(modelFile).channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
            Timber.d("Loaded model: ${modelMetadata.filePath} (${buffer.capacity()} bytes)")
            buffer
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading model: ${modelMetadata.filePath}")
            null
        }
    }
    
    /**
     * Execute inference
     * Input array must be properly formatted float array
     * Output is array of float values (logits, class probabilities, etc.)
     */
    override suspend fun infer(
        input: FloatArray,
        timeoutMs: Long
    ): FloatArray? {
        return withContext(Dispatchers.Default) {
            try {
                val interp = interpreter
                if (interp == null) {
                    Timber.w("Interpreter not initialized for ${modelMetadata.type}")
                    return@withContext null
                }
                
                val inputBuf = inputBuffer
                if (inputBuf == null) {
                    Timber.w("Input buffer not allocated for ${modelMetadata.type}")
                    return@withContext null
                }
                
                val outputBuf = outputBuffer
                if (outputBuf == null) {
                    Timber.w("Output buffer not allocated for ${modelMetadata.type}")
                    return@withContext null
                }
                
                // Copy input into buffer
                System.arraycopy(input, 0, inputBuf, 0, minOf(input.size, inputBuf.size))
                
                // Execute inference (synchronous, may block)
                val startTime = System.currentTimeMillis()
                interp.run(inputBuf, outputBuf)
                val elapsed = System.currentTimeMillis() - startTime
                
                if (elapsed > timeoutMs) {
                    Timber.w("Inference took ${elapsed}ms, exceeded timeout ${timeoutMs}ms")
                }
                
                // Return copy of output
                return@withContext outputBuf.copyOf()
                
            } catch (e: Exception) {
                Timber.e(e, "Error executing inference for ${modelMetadata.type}")
                return@withContext null
            }
        }
    }
    
    /**
     * Cleanup resources
     * Safe to call multiple times
     */
    override suspend fun cleanup() {
        withContext(Dispatchers.Default) {
            try {
                interpreter?.close()
                interpreter = null
                inputBuffer = null
                outputBuffer = null
                Timber.d("Cleaned up interpreter for ${modelMetadata.type}")
            } catch (e: Exception) {
                Timber.w(e, "Error cleaning up interpreter")
            }
        }
    }
    
    override fun isInitialized(): Boolean {
        return interpreter != null && inputBuffer != null && outputBuffer != null
    }
}
