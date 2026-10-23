package com.kazun.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kazun.editor.ui.AppRoot
import com.kazun.editor.ui.theme.KazunTheme
import com.kazun.editor.ui.theme.Surface0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KazunTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Surface0) {
                    AppRoot()
                }
            }
        }
    }
}
