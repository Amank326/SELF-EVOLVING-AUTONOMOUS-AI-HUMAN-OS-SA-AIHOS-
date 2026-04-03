package com.aihos.replay.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihos.replay.engine.*
import com.aihos.replay.model.*

// ─────────────────────────────────────────────────────────────────────────────
// PART 5 — TIMELINE UI
// Compose-based timeline slider with scrubbing, event markers,
// cognitive spike visualization, and playback controls.
// ─────────────────────────────────────────────────────────────────────────────

// ═════════════════════════════════════════════════════════════════════════════
// COLOR PALETTE
// ═════════════════════════════════════════════════════════════════════════════

private val ReplayDarkBg = Color(0xFF0A0E1A)
private val ReplayPanelBg = Color(0xFF121829)
private val ReplayCyan = Color(0xFF00D4FF)
private val ReplayViolet = Color(0xFF9B59F0)
private val ReplayGold = Color(0xFFFFD740)
private val ReplayRed = Color(0xFFFF4B5C)
private val ReplayGreen = Color(0xFF4BFFB5)
private val ReplayTextPrimary = Color(0xFFE8EAF6)
private val ReplayTextSecondary = Color(0xFF7986CB)
private val TimelineTrackColor = Color(0xFF1A2340)
private val TimelineProgressColor = Color(0xFF00B8D4)

// ═════════════════════════════════════════════════════════════════════════════
// MAIN REPLAY PANEL
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Full replay debug UI panel — overlays at the bottom of the 3D view.
 */
@Composable
fun ReplayPanel(
    playbackState: PlaybackState,
    timelinePosition: Float,
    currentEvent: ReplayEvent?,
    currentDiff: StateDiff?,
    markers: List<TimelineMarker>,
    totalFrames: Long,
    currentSequence: Long,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onExitReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, ReplayDarkBg.copy(alpha = 0.95f)),
                    startY = 0f,
                    endY = 60f
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Replay indicator
            val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    tween(800, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ReplayCyan.copy(alpha = pulseAlpha))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "REPLAY MODE",
                color = ReplayCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.weight(1f))

            // Frame counter
            Text(
                "${currentSequence} / ${totalFrames}",
                color = ReplayTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(12.dp))

            // Expand/collapse
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = "Toggle",
                    tint = ReplayTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Exit replay
            IconButton(
                onClick = onExitReplay,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Exit Replay",
                    tint = ReplayRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Timeline Slider ──────────────────────────────────────────────
        TimelineSlider(
            position = timelinePosition,
            markers = markers,
            onSeek = onSeek
        )

        // ── Expanded details ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(8.dp))

                // Playback controls
                PlaybackControls(
                    state = playbackState,
                    speed = playbackSpeed,
                    onPlay = onPlay,
                    onPause = onPause,
                    onStepForward = onStepForward,
                    onStepBackward = onStepBackward,
                    onSpeedChange = { speed ->
                        playbackSpeed = speed
                        onSpeedChange(speed)
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Event info card
                currentEvent?.let { event ->
                    EventInfoCard(event, currentDiff)
                }
            }
        }
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// TIMELINE SLIDER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TimelineSlider(
    position: Float,
    markers: List<TimelineMarker>,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(TimelineTrackColor)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normalized = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(normalized)
                }
            }
            .drawBehind {
                drawTimeline(position, markers)
            }
    ) {
        // Playhead
        Box(
            modifier = Modifier
                .offset(x = (position * (this@Box as BoxScope).run { 0 }).dp) // Handled in drawBehind
                .fillMaxHeight()
        )
    }
}

/**
 * Custom drawing for the timeline track, markers, and progress bar.
 */
private fun DrawScope.drawTimeline(position: Float, markers: List<TimelineMarker>) {
    val w = size.width
    val h = size.height

    // ── Progress bar ─────────────────────────────────────────────────────
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                TimelineProgressColor.copy(alpha = 0.8f),
                TimelineProgressColor.copy(alpha = 0.3f)
            )
        ),
        size = Size(w * position, h)
    )

    // ── Markers ──────────────────────────────────────────────────────────
    for (marker in markers) {
        val x = marker.position * w
        val color = when (marker.type) {
            TimelineMarkerType.COGNITIVE_SPIKE -> Color(0xFFFFD740) // Gold
            TimelineMarkerType.EVOLUTION -> Color(0xFF9B59F0)       // Violet
            TimelineMarkerType.ANOMALY -> Color(0xFFFF4B5C)         // Red
            TimelineMarkerType.BOOKMARK -> Color(0xFF4BFFB5)        // Green
            TimelineMarkerType.DECISION -> Color(0xFF00D4FF)        // Cyan
        }

        // Marker line
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 2f
        )

        // Marker dot
        drawCircle(
            color = color,
            radius = 4f,
            center = Offset(x, h * 0.3f)
        )
    }

    // ── Playhead ─────────────────────────────────────────────────────────
    val playheadX = position * w
    drawLine(
        color = Color.White,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, h),
        strokeWidth = 2.5f
    )
    drawCircle(
        color = Color.White,
        radius = 6f,
        center = Offset(playheadX, h / 2)
    )
}


// ═════════════════════════════════════════════════════════════════════════════
// PLAYBACK CONTROLS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PlaybackControls(
    state: PlaybackState,
    speed: Float,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Step backward
        IconButton(onClick = onStepBackward, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Step Back",
                tint = ReplayTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Play / Pause
        FilledIconButton(
            onClick = { if (state == PlaybackState.PLAYING) onPause() else onPlay() },
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = ReplayCyan.copy(alpha = 0.2f)
            )
        ) {
            Icon(
                if (state == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state == PlaybackState.PLAYING) "Pause" else "Play",
                tint = ReplayCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Step forward
        IconButton(onClick = onStepForward, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Step Forward",
                tint = ReplayTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(20.dp))

        // Speed control
        val speeds = listOf(0.25f, 0.5f, 1.0f, 2.0f, 4.0f)
        speeds.forEach { s ->
            val isActive = speed == s
            TextButton(
                onClick = { onSpeedChange(s) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    "${s}x",
                    color = if (isActive) ReplayCyan else ReplayTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


// ═════════════════════════════════════════════════════════════════════════════
// EVENT INFO CARD
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun EventInfoCard(
    event: ReplayEvent,
    diff: StateDiff?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ReplayPanelBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Event type badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (typeColor, typeLabel) = eventTypeDisplay(event.eventType)
                Box(
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTimestamp(event.timestamp),
                    color = ReplayTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(8.dp))

            // Cognitive metrics
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricChip("Load", "${(event.cognitiveSnapshot.cognitiveLoad * 100).toInt()}%", ReplayCyan)
                Spacer(Modifier.width(8.dp))
                MetricChip("Confidence", "${(event.cognitiveSnapshot.confidenceLevel * 100).toInt()}%", ReplayGreen)
                Spacer(Modifier.width(8.dp))
                MetricChip("Rules", "${event.cognitiveSnapshot.activeRuleCount}", ReplayViolet)
            }

            // Decision details
            event.decisionSnapshot?.let { decision ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Action: ${decision.action}",
                    color = ReplayTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (decision.reasoningScores.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Options: ${decision.reasoningScores.joinToString { "${it.action}(${(it.score * 100).toInt()}%)" }}",
                        color = ReplayTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Diff summary
            diff?.let { d ->
                if (d.totalChanges > 0) {
                    Spacer(Modifier.height(8.dp))
                    DiffSummaryRow(d)
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "$label: $value",
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun DiffSummaryRow(diff: StateDiff) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (diff.addedNodes.isNotEmpty()) {
            DiffBadge("+${diff.addedNodes.size} nodes", ReplayGreen)
            Spacer(Modifier.width(6.dp))
        }
        if (diff.removedNodeIds.isNotEmpty()) {
            DiffBadge("-${diff.removedNodeIds.size} nodes", ReplayRed)
            Spacer(Modifier.width(6.dp))
        }
        if (diff.changedNodes.isNotEmpty()) {
            DiffBadge("~${diff.changedNodes.size} changed", ReplayGold)
            Spacer(Modifier.width(6.dp))
        }
        if (diff.ruleChanges.isNotEmpty()) {
            DiffBadge("${diff.ruleChanges.size} rules", ReplayViolet)
        }
    }
}

@Composable
private fun DiffBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// UTILITIES
// ═════════════════════════════════════════════════════════════════════════════

private fun eventTypeDisplay(type: ReplayEventType): Pair<Color, String> {
    return when (type) {
        ReplayEventType.DECISION_CYCLE      -> ReplayCyan to "DECISION"
        ReplayEventType.REFLECTION          -> ReplayViolet to "REFLECTION"
        ReplayEventType.EVOLUTION           -> ReplayGold to "EVOLUTION"
        ReplayEventType.MEMORY_STORE        -> ReplayGreen to "MEMORY STORE"
        ReplayEventType.MEMORY_RECALL       -> Color(0xFF26C6DA) to "RECALL"
        ReplayEventType.COGNITIVE_SPIKE     -> ReplayRed to "SPIKE"
        ReplayEventType.SYSTEM_STATE_CHANGE -> Color(0xFF78909C) to "STATE CHANGE"
        ReplayEventType.ANOMALY             -> ReplayRed to "ANOMALY"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val fmt = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
    return fmt.format(date)
}
