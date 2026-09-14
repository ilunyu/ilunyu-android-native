package com.ilunyu.lunyu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ilunyu.lunyu.ui.MainScreen
import com.ilunyu.lunyu.ui.MainViewModel
import com.ilunyu.lunyu.ui.theme.LunyuTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val fontPreference by viewModel.fontPreference.collectAsState()

            LunyuTheme(
                themeMode = themeMode,
                fontPreference = fontPreference
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
