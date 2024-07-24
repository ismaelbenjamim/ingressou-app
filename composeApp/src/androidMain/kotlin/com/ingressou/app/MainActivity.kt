package com.ingressou.app

import AndroidSessionManager
import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = AndroidSessionManager(this)

        setContent {
            App(sessionManager)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val context = MainActivity()
    val sessionManager = AndroidSessionManager(context)
    App(sessionManager)
}
