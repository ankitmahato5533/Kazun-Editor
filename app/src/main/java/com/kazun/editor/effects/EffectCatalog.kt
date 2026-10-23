package com.kazun.editor.effects

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Loads the generated catalog (assets/effects/catalog.json) once and offers search.
 * The catalog is produced by tools/generate_catalog.mjs — 1,256 entries built from
 * 156 primitives and data presets.
 */
class EffectCatalog private constructor(val effects: List<EffectDescriptor>) {

    private val byId = effects.associateBy { it.id }

    fun get(id: String): EffectDescriptor? = byId[id]

    val categories: List<EffectCategory> = CATEGORY_ORDER.mapNotNull { (id, title) ->
        if (effects.any { it.category == id }) EffectCategory(id, title, "") else null
    }

    fun inCategory(category: String): List<EffectDescriptor> =
        effects.filter { it.category == category }.sortedBy { it.name }

    fun search(query: String): List<EffectDescriptor> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return effects.asSequence()
            .map { e -> e to score(e, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(60)
            .map { it.first }
            .toList()
    }

    private fun score(e: EffectDescriptor, q: String): Int {
        val name = e.name.lowercase()
        return when {
            name == q -> 100
            name.startsWith(q) -> 80
            name.contains(q) -> 60
            e.category.contains(q) -> 40
            e.keywords.any { it.contains(q) } -> 30
            else -> 0
        }
    }

    companion object {
        private val CATEGORY_ORDER = listOf(
            "luts" to "LUTs & Filters",
            "looks" to "Quick Looks",
            "transitions" to "Transitions",
            "textanim" to "Text Animations",
            "textstyle" to "Text Styles",
            "motion" to "Motion",
            "particles" to "Particles",
            "stickers" to "Stickers",
            "audio" to "Audio",
            "blur" to "Blur",
            "glitch" to "Glitch",
            "light" to "Light",
            "distort" to "Distortion",
            "detail" to "Detail",
            "gradient" to "Gradient Maps",
            "split" to "Split Screen",
            "border" to "Borders",
            "chroma" to "Chroma Key",
            "speed" to "Speed",
            "ai" to "AI"
        )

        @Volatile private var cached: EffectCatalog? = null

        fun load(context: Context): EffectCatalog = cached ?: synchronized(this) {
            cached ?: run {
                val text = context.assets.open("effects/catalog.json").bufferedReader().use { it.readText() }
                val file = Json { ignoreUnknownKeys = true }.decodeFromString(CatalogFile.serializer(), text)
                EffectCatalog(file.effects).also { cached = it }
            }
        }
    }
}
