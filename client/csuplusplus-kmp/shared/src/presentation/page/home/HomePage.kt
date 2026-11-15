package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomePage(viewModel: HomeViewModel = koinViewModel()) {
    Text(viewModel.greeting)
}
