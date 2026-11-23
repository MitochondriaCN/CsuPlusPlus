package ink.qingshuige.csuplusplus

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ink.qingshuige.csuplusplus.presentation.App
import ink.qingshuige.csuplusplus.resources.Res
import ink.qingshuige.csuplusplus.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CSU++",
        icon = painterResource(Res.drawable.app_icon),
    ) {
        App()
    }
}
