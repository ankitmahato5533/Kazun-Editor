package com.kazun.editor.features.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kazun.editor.effects.EffectCatalog
import com.kazun.editor.effects.EffectDefaults
import com.kazun.editor.effects.EffectDescriptor
import com.kazun.editor.effects.ParamDef
import com.kazun.editor.model.EffectInstance
import com.kazun.editor.model.Project
import com.kazun.editor.timeline.TimelineOps
import com.kazun.editor.ui.theme.AccentAmber
import com.kazun.editor.ui.theme.AccentTeal
import com.kazun.editor.ui.theme.KazunRed
import com.kazun.editor.ui.theme.Surface1
import com.kazun.editor.ui.theme.Surface2
import com.kazun.editor.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectsSheet(
    catalog: EffectCatalog,
    project: Project,
    selectedClipId: String?,
    onDismiss: () -> Unit,
    onApply: (Project) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clip = selectedClipId?.let { TimelineOps.findClip(project, it) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(catalog.categories.firstOrNull()?.id ?: "luts") }
    var openEffectInstance by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Effects",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${catalog.effects.size} total",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close")
                }
            }

            if (clip == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Select a clip on the timeline first", color = TextSecondary)
                }
            } else {

                // search
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search 1,256 effects…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))

                if (query.isBlank()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(catalog.categories.size) { index ->
                            val cat = catalog.categories[index]
                            FilterChip(
                                selected = category == cat.id,
                                onClick = { category = cat.id },
                                label = { Text(cat.title) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                val list = remember(query, category) {
                    if (query.isBlank()) catalog.inCategory(category) else catalog.search(query)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(list, key = { it.id }) { descriptor ->
                        EffectTile(descriptor) {
                            val instance = EffectDefaults.instanceOf(descriptor)
                            onApply(TimelineOps.addEffect(project, clip.id, instance))
                            openEffectInstance = instance.id
                        }
                    }
                }

                if (clip.effects.isEmpty()) {
                    Text(
                        "No effects on this clip yet. Tap one above to add.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("Stack (${clip.effects.size}/6)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        clip.effects.forEach { effect ->
                            val descriptor = catalog.get(effect.effectId)
                            EffectStackRow(
                                effect = effect,
                                descriptor = descriptor,
                                project = project,
                                clipId = clip.id,
                                expanded = openEffectInstance == effect.id,
                                onToggleExpand = {
                                    openEffectInstance = if (openEffectInstance == effect.id) null else effect.id
                                },
                                onApply = onApply
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EffectTile(descriptor: EffectDescriptor, onClick: () -> Unit) {
    Box(
        Modifier
            .height(56.dp)
            .background(Surface2, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Text(
            descriptor.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            fontWeight = FontWeight.Medium
        )
        val tint = when (descriptor.cost) {
            "heavy" -> KazunRed
            "medium" -> AccentAmber
            else -> AccentTeal
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .width(6.dp)
                .height(6.dp)
                .background(tint, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun EffectStackRow(
    effect: EffectInstance,
    descriptor: EffectDescriptor?,
    project: Project,
    clipId: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onApply: (Project) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                descriptor?.name ?: effect.effectId,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onApply(TimelineOps.removeEffect(project, clipId, effect.id)) }) {
                Text("Remove", color = KazunRed, style = MaterialTheme.typography.labelSmall)
            }
        }

        if (expanded && descriptor != null) {
            Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                // L1 — universal intensity
                Text("Intensity", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Slider(
                    value = effect.intensity,
                    onValueChange = { v ->
                        onApply(TimelineOps.updateEffect(project, clipId, effect.copy(intensity = v)))
                    },
                    valueRange = 0f..1f
                )
                // L2 — generated from descriptor
                descriptor.params.forEach { param ->
                    ParamRow(param, effect) { updated ->
                        onApply(TimelineOps.updateEffect(project, clipId, updated))
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamRow(param: ParamDef, effect: EffectInstance, onChange: (EffectInstance) -> Unit) {
    when (param.type) {
        "ENUM" -> {
            Text(param.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(param.options.size) { i ->
                    val option = param.options[i]
                    FilterChip(
                        selected = (effect.options[param.key] ?: param.defaultOption) == option,
                        onClick = {
                            onChange(effect.copy(options = effect.options + (param.key to option)))
                        },
                        label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        "BOOL" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(param.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Switch(
                    checked = (effect.params[param.key] ?: param.default) > 0.5f,
                    onCheckedChange = { v ->
                        onChange(effect.copy(params = effect.params + (param.key to if (v) 1f else 0f)))
                    }
                )
            }
        }

        else -> {
            val value = effect.params[param.key] ?: param.default
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    param.label,
                    Modifier.width(84.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Slider(
                    value = value.coerceIn(param.min, param.max),
                    onValueChange = { v ->
                        onChange(effect.copy(params = effect.params + (param.key to v)))
                    },
                    valueRange = param.min..param.max,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "%.2f".format(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}
