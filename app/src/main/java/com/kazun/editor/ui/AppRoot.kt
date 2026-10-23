package com.kazun.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kazun.editor.data.ProjectStore
import com.kazun.editor.features.editor.EditorScreen
import com.kazun.editor.features.projects.ProjectsScreen

sealed interface Screen {
    data object Projects : Screen
    data class Editor(val projectId: String) : Screen
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store = remember { ProjectStore(context) }
    var screen by remember { mutableStateOf<Screen>(Screen.Projects) }

    LaunchedEffect(Unit) { store.load() }

    when (val s = screen) {
        is Screen.Projects -> ProjectsScreen(
            store = store,
            onOpenProject = { screen = Screen.Editor(it) }
        )

        is Screen.Editor -> EditorScreen(
            store = store,
            projectId = s.projectId,
            onBack = { screen = Screen.Projects }
        )
    }
}
