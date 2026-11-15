package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    var greeting by mutableStateOf("欢迎使用 CSU++")
        private set
}
