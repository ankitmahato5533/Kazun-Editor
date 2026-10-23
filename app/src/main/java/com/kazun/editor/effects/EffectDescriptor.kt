package com.kazun.editor.effects

import kotlinx.serialization.Serializable

@Serializable
data class ParamDef(
    val key: String,
    val label: String,
    val type: String = "FLOAT",          // FLOAT | INT | BOOL | COLOR | ENUM | ANGLE | POINT2D | SEED
    val default: Float = 0f,
    val min: Float = 0f,
    val max: Float = 1f,
    val step: Float = 0.01f,
    val group: String = "General",
    val options: List<String> = emptyList(),
    val defaultOption: String = "",
    val defColor: String = "#FFFFFF"
)

@Serializable
data class EffectDescriptor(
    val id: String,
    val name: String,
    val category: String,
    val cost: String = "cheap",          // cheap | medium | heavy
    val description: String = "",
    val params: List<ParamDef> = emptyList(),
    val keywords: List<String> = emptyList()
)

@Serializable
data class CatalogFile(val version: Int = 1, val effects: List<EffectDescriptor> = emptyList())

data class EffectCategory(val id: String, val title: String, val icon: String)
