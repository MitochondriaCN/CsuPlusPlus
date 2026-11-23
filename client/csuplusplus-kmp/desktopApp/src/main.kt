package ink.qingshuige.csuplusplus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ink.qingshuige.csuplusplus.presentation.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "CSU++") {
        App()
    }
}
