package com.kazun.editor.features.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kazun.editor.model.Clip
import com.kazun.editor.model.Project
import com.kazun.editor.model.Track
import com.kazun.editor.model.TrackType
import com.kazun.editor.ui.theme.AccentAmber
import com.kazun.editor.ui.theme.AccentTeal
import com.kazun.editor.ui.theme.KazunRed
import com.kazun.editor.ui.theme.Surface0
import com.kazun.editor.ui.theme.Surface2
import com.kazun.editor.ui.theme.Surface3
import com.kazun.editor.ui.theme.TextSecondary
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Custom timeline. Zoom is expressed as dp per millisecond; all rows share one
 * scroll state so they stay aligned, and the playhead lives in the same
 * scrollable content so it tracks perfectly.
 */
@Composable
fun TimelineView(
    project: Project,
    selectedClipId: String?,
    playheadMs: Long,
    dpPerMs: Float,
    onSelectClip: (String?) -> Unit,
    onSeek: (Long) -> Unit,
    onMoveClip: (String, Long) -> Unit,
    onTrimStart: (String, Long) -> Unit,
    onTrimEnd: (String, Long) -> Unit,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current.density
    val visibleTracks = remember(project) {
        project.tracks.filter { it.type != TrackType.OVERLAY }.take(4)
    }
    val totalMs = (project.durationMs + 4000).coerceAtLeast(12_000)
    val contentWidth: Dp = (totalMs * dpPerMs).dp

    Column(Modifier.fillMaxWidth()) {
        // ---------- ruler ----------
        Row {
            Spacer(Modifier.width(HEADER_WIDTH.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .horizontalScroll(scroll)
            ) {
                Box(
                    Modifier
                        .width(contentWidth)
                        .height(22.dp)
                        .pointerInput(dpPerMs, density) {
                            detectTapGestures { offset ->
                                onSeek((offset.x / (dpPerMs * density)).roundToLong().coerceAtLeast(0))
                            }
                        }
                ) {
                    val ticks = remember(dpPerMs, totalMs) {
                        val step = stepForZoom(dpPerMs)
                        generateSequence(0L) { it + step }.takeWhile { it < totalMs }.toList()
                    }
                    ticks.forEach { t ->
                        Text(
                            text = formatTick(t),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.offset(x = (t * dpPerMs).dp)
                        )
                    }
                }
            }
        }

        // ---------- tracks ----------
        visibleTracks.forEach { track ->
            Row(Modifier.padding(vertical = 2.dp)) {
                TrackHeader(track)
                Box(
                    Modifier
                        .weight(1f)
                        .height(TRACK_HEIGHT)
                        .horizontalScroll(scroll)
                ) {
                    Box(
                        Modifier
                            .width(contentWidth)
                            .height(TRACK_HEIGHT)
                            .background(Surface0)
                    ) {
                        track.clips.forEach { clip ->
                            ClipView(
                                clip = clip,
                                selected = clip.id == selectedClipId,
                                dpPerMs = dpPerMs,
                                density = density,
                                onSelect = { onSelectClip(clip.id) },
                                onMove = { deltaMs -> onMoveClip(clip.id, (clip.timelineStartMs + deltaMs).coerceAtLeast(0)) },
                                onTrimStart = { deltaMs ->
                                    onTrimStart(clip.id, clip.trimStartMs + (deltaMs * clip.speed).roundToLong())
                                },
                                onTrimEnd = { deltaMs ->
                                    onTrimEnd(clip.id, clip.trimEndMs + (deltaMs * clip.speed).roundToLong())
                                }
                            )
                        }
                        Playhead(
                            playheadMs = playheadMs,
                            dpPerMs = dpPerMs,
                            height = TRACK_HEIGHT,
                            density = density
                        )
                    }
                }
            }
        }
    }
}

private const val HEADER_WIDTH = 40
private val TRACK_HEIGHT = 54.dp

@Composable
private fun TrackHeader(track: Track) {
    Box(
        Modifier
            .width(HEADER_WIDTH.dp)
            .height(TRACK_HEIGHT)
            .background(Surface2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (track.type) {
                TrackType.VIDEO -> "V"
                TrackType.AUDIO -> "A"
                TrackType.TEXT -> "T"
                else -> "O"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (track.type) {
                TrackType.VIDEO -> KazunRed
                TrackType.AUDIO -> AccentTeal
                TrackType.TEXT -> AccentAmber
                else -> TextSecondary
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ClipView(
    clip: Clip,
    selected: Boolean,
    dpPerMs: Float,
    density: Float,
    onSelect: () -> Unit,
    onMove: (Long) -> Unit,
    onTrimStart: (Long) -> Unit,
    onTrimEnd: (Long) -> Unit,
) {
    val width = (clip.durationMs * dpPerMs).dp.coerceAtLeast(18.dp)
    val accent = when (clip.kind) {
        com.kazun.editor.model.ClipKind.TEXT -> AccentAmber
        else -> KazunRed
    }

    Box(
        Modifier
            .offset { IntOffset((clip.timelineStartMs * dpPerMs * density).roundToInt(), 0) }
            .width(width)
            .height(TRACK_HEIGHT)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accent.copy(alpha = 0.42f) else Surface3)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(clip.id, dpPerMs) {
                detectTapGestures { onSelect() }
            }
            .pointerInput(clip.id, dpPerMs, density) {
                var accumulated = 0f
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f }
                ) { change, drag ->
                    change.consume()
                    accumulated += drag.x
                    val deltaMs = (accumulated / (dpPerMs * density)).roundToLong()
                    if (kotlin.math.abs(deltaMs) >= 10) {
                        onMove(deltaMs)
                        accumulated = 0f
                    }
                }
            }
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(10.dp)
                .height(TRACK_HEIGHT)
                .pointerInput(clip.id, dpPerMs, density) {
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { onSelect() },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f }
                    ) { change, drag ->
                        change.consume()
                        acc += drag.x
                        val deltaMs = (acc / (dpPerMs * density)).roundToLong()
                        if (kotlin.math.abs(deltaMs) >= 10) {
                            onTrimStart(deltaMs)
                            acc = 0f
                        }
                    }
                }
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(10.dp)
                .height(TRACK_HEIGHT)
                .pointerInput(clip.id, dpPerMs, density) {
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { onSelect() },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f }
                    ) { change, drag ->
                        change.consume()
                        acc += drag.x
                        val deltaMs = (acc / (dpPerMs * density)).roundToLong()
                        if (kotlin.math.abs(deltaMs) >= 10) {
                            onTrimEnd(deltaMs)
                            acc = 0f
                        }
                    }
                }
        )
        Text(
            text = "%.1fs".format(clip.durationMs / 1000f),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 12.dp),
            maxLines = 1
        )
        if (clip.effects.isNotEmpty()) {
            Text(
                text = "fx ${clip.effects.size}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentTeal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
            )
        }
    }
}

@Composable
private fun Playhead(playheadMs: Long, dpPerMs: Float, height: Dp, density: Float) {
    val x = (playheadMs * dpPerMs * density).roundToInt()
    Box(
        Modifier
            .offset { IntOffset(x, 0) }
            .width(2.dp)
            .height(height)
            .background(KazunRed)
    )
    Box(
        Modifier
            .offset { IntOffset(x - 4, 0) }
            .size(9.dp)
            .background(KazunRed, RoundedCornerShape(2.dp))
    )
}

private fun stepForZoom(dpPerMs: Float): Long = when {
    dpPerMs > 0.20f -> 500
    dpPerMs > 0.08f -> 1_000
    dpPerMs > 0.03f -> 2_000
    dpPerMs > 0.012f -> 5_000
    else -> 10_000
}

private fun formatTick(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
