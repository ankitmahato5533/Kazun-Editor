package com.kazun.editor.features.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kazun.editor.data.ProjectStore
import com.kazun.editor.effects.EffectCatalog
import com.kazun.editor.effects.EffectMapper
import com.kazun.editor.export.TransformerExporter
import com.kazun.editor.media.MediaProbe
import com.kazun.editor.model.Clip
import com.kazun.editor.model.ClipKind
import com.kazun.editor.model.Project
import com.kazun.editor.model.TextProps
import com.kazun.editor.model.TrackType
import com.kazun.editor.timeline.TimelineOps
import com.kazun.editor.ui.theme.AccentAmber
import com.kazun.editor.ui.theme.AccentTeal
import com.kazun.editor.ui.theme.KazunRed
import com.kazun.editor.ui.theme.Surface0
import com.kazun.editor.ui.theme.Surface1
import com.kazun.editor.ui.theme.Surface2
import com.kazun.editor.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun EditorScreen(store: ProjectStore, projectId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stored by store.projects.collectAsState()

    var project by remember { mutableStateOf<Project?>(null) }
    var undoStack by remember { mutableStateOf<List<Project>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<Project>>(emptyList()) }
    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var playheadMs by remember { mutableStateOf(0L) }
    var dpPerMs by remember { mutableStateOf(0.035f) }
    var isPlaying by remember { mutableStateOf(false) }
    var showEffects by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var lastExportFile by remember { mutableStateOf<File?>(null) }
    var loadedUri by remember(exoPlayer) { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId, stored) {
        if (project == null) project = stored.firstOrNull { it.id == projectId }
    }

    val catalog = remember { EffectCatalog.load(context) }
    val canvasClip = remember(project, playheadMs) {
        project?.let { p ->
            p.tracks.firstOrNull { it.type == TrackType.VIDEO }?.let { TimelineOps.clipAt(it, playheadMs) }
        }
    }
    val stackSignature = EffectMapper.stackSignature(canvasClip)
    val previewEffects = remember(stackSignature) {
        canvasClip?.let { EffectMapper.mapClip(context, it, catalog) } ?: emptyList()
    }
    // Media3 applies one effect chain per player, so the player is rebuilt when the
    // active clip's stack changes. Brief pause at clip boundaries is expected.
    val exoPlayer = remember(stackSignature) {
        ExoPlayer.Builder(context).apply {
            if (previewEffects.isNotEmpty()) setVideoEffects(previewEffects)
        }.build()
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    fun commit(newProject: Project, recordUndo: Boolean = true) {
        val old = project ?: return
        if (newProject == old) return
        if (recordUndo) {
            undoStack = (undoStack + old).takeLast(60)
            redoStack = emptyList()
        }
        project = newProject
        scope.launch {
            delay(400)
            store.save(newProject)
        }
    }

    val current: Project? = project
    val videoTrack = current?.tracks?.firstOrNull { it.type == TrackType.VIDEO }

    fun clipAtPlayhead(): Clip? = videoTrack?.let { TimelineOps.clipAt(it, playheadMs) }
        ?: videoTrack?.clips?.lastOrNull { it.timelineEndMs <= playheadMs }

    // keep the player on the clip under the playhead
    LaunchedEffect(current, current?.let { p -> clipAtPlayhead()?.id }, playheadMs, isPlaying) {
        val clip = clipAtPlayhead() ?: return@LaunchedEffect
        if (clip.sourceUri.isEmpty()) return@LaunchedEffect
        if (loadedUri != clip.sourceUri) {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(clip.sourceUri)))
            exoPlayer.prepare()
            loadedUri = clip.sourceUri
        }
        val localMs = (((playheadMs - clip.timelineStartMs) * clip.speed).toLong()).coerceAtLeast(0L)
        if (!isPlaying && kotlin.math.abs(exoPlayer.currentPosition - localMs) > 120) {
            exoPlayer.seekTo(localMs)
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        while (isActive && isPlaying) {
            delay(60)
            val clip = clipAtPlayhead()
            if (clip == null) {
                isPlaying = false
            } else {
                val localMs = exoPlayer.currentPosition
                playheadMs = clip.timelineStartMs + (localMs / clip.speed).toLong()
                if (localMs >= clip.durationMs - 40) {
                    val next = videoTrack?.clips?.firstOrNull { it.timelineStartMs >= clip.timelineEndMs }
                    if (next != null) {
                        playheadMs = next.timelineStartMs
                    } else {
                        isPlaying = false
                        exoPlayer.pause()
                    }
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val p = project ?: return@rememberLauncherForActivityResult
        val track = p.tracks.firstOrNull { it.type == TrackType.VIDEO } ?: return@rememberLauncherForActivityResult
        scope.launch {
            val metas = withContext(Dispatchers.IO) { uris.map { MediaProbe.probe(context, it) to it } }
            var updated = project ?: return@launch
            var cursor = updated.tracks.first { it.id == track.id }.durationMs
            metas.forEach { (meta, uri) ->
                if (meta.durationMs <= 0) return@forEach
                val clip = Clip(
                    sourceUri = uri.toString(),
                    displayName = meta.displayName,
                    sourceDurationMs = meta.durationMs,
                    trimEndMs = meta.durationMs,
                    timelineStartMs = cursor,
                    kind = ClipKind.VIDEO
                )
                updated = TimelineOps.addClip(updated, track.id, clip)
                cursor = updated.tracks.first { it.id == track.id }.durationMs
            }
            commit(updated)
        }
    }

    fun addText(content: String) {
        val p = project ?: return
        val track = p.tracks.firstOrNull { it.type == TrackType.TEXT } ?: return
        val clip = Clip(
            sourceUri = "",
            sourceDurationMs = 3000,
            trimEndMs = 3000,
            timelineStartMs = playheadMs,
            kind = ClipKind.TEXT,
            text = TextProps(content = content)
        )
        commit(TimelineOps.addClip(p, track.id, clip))
    }

    fun doExport() {
        val p = project ?: return
        exporting = true
        exportMessage = null
        scope.launch {
            val result = runCatching {
                val outDir = File(context.filesDir, "exports").apply { mkdirs() }
                val out = File(outDir, "kazun_${System.currentTimeMillis()}.mp4")
                TransformerExporter(context).export(p, catalog, out) { pct ->
                    progress = pct
                }
            }
            exporting = false
            exportMessage = result.fold(
                onSuccess = { file ->
                    lastExportFile = file
                    "Exported: ${file.name} (${file.length() / 1024 / 1024} MB)"
                },
                onFailure = { "Export failed: ${it.message}" }
            )
        }
    }

    Column(Modifier.fillMaxSize().background(Surface0)) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface0),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    current?.name ?: "Loading",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            actions = {
                IconButton(
                    enabled = undoStack.isNotEmpty(),
                    onClick = {
                        val prev = undoStack.last()
                        undoStack = undoStack.dropLast(1)
                        project?.let { redoStack = (redoStack + it).takeLast(60) }
                        project = prev
                    }
                ) { Icon(Icons.Filled.Undo, contentDescription = "Undo") }
                IconButton(
                    enabled = redoStack.isNotEmpty(),
                    onClick = {
                        val next = redoStack.last()
                        redoStack = redoStack.dropLast(1)
                        project?.let { undoStack = (undoStack + it).takeLast(60) }
                        project = next
                    }
                ) { Icon(Icons.Filled.Redo, contentDescription = "Redo") }
                IconButton(onClick = { showEffects = true }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Effects", tint = AccentTeal)
                }
            }
        )

        // ---- preview ----
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .aspectRatio(16f / 9f)
                .background(Color.Black, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            val clip = current?.let { p -> p.tracks.firstOrNull { it.type == TrackType.TEXT }?.let { TimelineOps.clipAt(it, playheadMs) } }
            if (clip?.text != null) {
                Text(
                    text = clip.text!!.content,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }

        // ---- transport ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isPlaying = !isPlaying }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                "${MediaProbe.formatDuration(playheadMs)} / ${MediaProbe.formatDuration(current?.durationMs ?: 0)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text("zoom", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            androidx.compose.material3.Slider(
                value = dpPerMs,
                onValueChange = { dpPerMs = it },
                valueRange = 0.008f..0.25f,
                modifier = Modifier.width(120.dp)
            )
        }

        // ---- tools ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { picker.launch(arrayOf("video/*")) }) {
                Icon(Icons.Filled.Add, "Add media", tint = AccentTeal)
            }
            IconButton(
                enabled = selectedClipId != null,
                onClick = {
                    val p = project ?: return@IconButton
                    val id = selectedClipId ?: return@IconButton
                    commit(TimelineOps.split(p, id, playheadMs))
                }
            ) { Icon(Icons.Filled.ContentCut, "Split", tint = AccentAmber) }
            IconButton(
                enabled = selectedClipId != null,
                onClick = {
                    val p = project ?: return@IconButton
                    val id = selectedClipId ?: return@IconButton
                    commit(TimelineOps.removeClip(p, id))
                    selectedClipId = null
                }
            ) { Icon(Icons.Filled.Delete, "Delete", tint = KazunRed) }
            IconButton(onClick = { showTextDialog = true }) {
                Icon(Icons.Filled.TextFields, "Text", tint = AccentAmber)
            }
            Spacer(Modifier.weight(1f))
            Text(
                selectedClipId?.let { "clip selected" } ?: "tap a clip",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        // ---- timeline ----
        Box(Modifier.weight(1f)) {
            val p = current
            if (p != null) {
                TimelineView(
                    project = p,
                    selectedClipId = selectedClipId,
                    playheadMs = playheadMs,
                    dpPerMs = dpPerMs,
                    onSelectClip = { selectedClipId = it },
                    onSeek = { ms -> playheadMs = ms.coerceAtLeast(0) },
                    onMoveClip = { id, start -> commit(TimelineOps.moveClip(p, id, start), recordUndo = false) },
                    onTrimStart = { id, v -> commit(TimelineOps.trimStart(p, id, v), recordUndo = false) },
                    onTrimEnd = { id, v -> commit(TimelineOps.trimEnd(p, id, v), recordUndo = false) }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading project…", color = TextSecondary)
                }
            }
        }

        if (exporting) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---- bottom bar ----
        Row(
            Modifier.fillMaxWidth().background(Surface1).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BottomAction("Effects", AccentTeal, Modifier.weight(1f)) { showEffects = true }
            BottomAction("Text", AccentAmber, Modifier.weight(1f)) { showTextDialog = true }
            BottomAction(
                if (exporting) "Exporting…" else "Export",
                KazunRed,
                Modifier.weight(1f)
            ) { if (!exporting) doExport() }
        }
    }

    if (showEffects && current != null) {
        EffectsSheet(
            catalog = catalog,
            project = current,
            selectedClipId = selectedClipId,
            onDismiss = { showEffects = false },
            onApply = { updated -> commit(updated) }
        )
    }

    if (showTextDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            containerColor = Surface1,
            title = { Text("Add text") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text overlay") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) addText(text)
                    showTextDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("Cancel") }
            }
        )
    }

    exportMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            containerColor = Surface1,
            title = { Text(if (msg.startsWith("Exported")) "Done" else "Export") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = {
                    val file = lastExportFile
                    if (msg.startsWith("Exported") && file != null && file.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${'$'}{context.packageName}.files", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share export"))
                    }
                    exportMessage = null
                }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = { exportMessage = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun BottomAction(
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .height(44.dp)
            .background(tint.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = tint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
    }
}
