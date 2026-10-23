package com.kazun.editor

import com.kazun.editor.effects.CatalogFile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {

    private val effects by lazy {
        val text = File("src/main/assets/effects/catalog.json").readText()
        Json { ignoreUnknownKeys = true }
            .decodeFromString(CatalogFile.serializer(), text).effects
    }

    @Test
    fun `catalog contains 1256 effects`() {
        assertEquals(1256, effects.size)
    }

    @Test
    fun `effect ids are unique`() {
        assertEquals(effects.size, effects.map { it.id }.toSet().size)
    }

    @Test
    fun `every effect has a category`() {
        effects.forEach { assertTrue("missing category: ${it.id}", it.category.isNotBlank()) }
    }

    @Test
    fun `parameter ranges are valid`() {
        effects.flatMap { it.params }.forEach { p ->
            assertTrue("${p.key} min>max on ${p.label}", p.min <= p.max)
            assertTrue("${p.key} default out of range", p.default in p.min..p.max)
        }
    }

    @Test
    fun `enum params have options`() {
        effects.flatMap { it.params }.filter { it.type == "ENUM" }.forEach {
            assertTrue("enum without options: ${it.key}", it.options.isNotEmpty())
        }
    }

    @Test
    fun `every category from the blueprint is populated`() {
        val expected = setOf(
            "luts", "looks", "transitions", "textanim", "textstyle", "motion", "particles",
            "stickers", "audio", "blur", "glitch", "light", "distort", "detail", "gradient",
            "split", "border", "chroma", "speed", "ai"
        )
        assertEquals(expected, effects.map { it.category }.toSet())
    }
}
