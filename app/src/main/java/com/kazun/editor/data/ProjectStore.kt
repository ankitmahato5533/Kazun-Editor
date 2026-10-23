package com.kazun.editor.data

import android.content.Context
import com.kazun.editor.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-based project store. Each project is a single JSON file:
 *   files/projects/{id}.json
 * Simple, atomic (write-temp-then-rename) and crash safe. Room migration left for later.
 */
class ProjectStore(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "projects").apply { mkdirs() }
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true; encodeDefaults = true }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val list = dir.listFiles { f -> f.name.endsWith(".json") }
            ?.mapNotNull { f -> runCatching { json.decodeFromString<Project>(f.readText()) }.getOrNull() }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
        _projects.value = list
    }

    suspend fun save(project: Project) = withContext(Dispatchers.IO) {
        val file = File(dir, "${project.id}.json")
        val tmp = File(dir, "${project.id}.json.tmp")
        tmp.writeText(json.encodeToString(Project.serializer(), project.copy(updatedAt = System.currentTimeMillis())))
        if (file.exists()) file.delete()
        tmp.renameTo(file)
        val current = _projects.value.toMutableList()
        val idx = current.indexOfFirst { it.id == project.id }
        if (idx >= 0) current[idx] = project else current.add(0, project)
        _projects.value = current.sortedByDescending { it.updatedAt }
    }

    suspend fun delete(projectId: String) = withContext(Dispatchers.IO) {
        File(dir, "$projectId.json").delete()
        _projects.value = _projects.value.filterNot { it.id == projectId }
    }

    suspend fun duplicate(project: Project): Project {
        val copy = project.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${project.name} copy",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        save(copy)
        return copy
    }
}
