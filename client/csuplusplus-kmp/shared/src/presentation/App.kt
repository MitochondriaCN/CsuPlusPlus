package ink.qingshuige.csuplusplus.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Lucide
import ink.qingshuige.csuplusplus.core.network.BindPlatformTokenStorageContext
import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.di.appModule
import ink.qingshuige.csuplusplus.presentation.page.auth.AuthPage
import ink.qingshuige.csuplusplus.presentation.page.home.HomePage
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    BindPlatformTokenStorageContext()
    KoinApplication(
        configuration = koinConfiguration { modules(appModule) }
    ) {
        AppContent()
    }
}

@Composable
private fun AppContent(
    tokenProvider: TokenProvider = koinInject(),
) {
    val isReady by tokenProvider.isReady.collectAsStateWithLifecycle()
    val token by tokenProvider.tokenFlow.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(NavigationDestinations.HOME) }

    LaunchedEffect(tokenProvider) {
        tokenProvider.awaitReady()
    }

    MaterialTheme {
        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (token == null) { // 没token先登录
            AuthPage(onFinished = {})
        } else {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    NavigationDestinations.entries.forEach {
                        item(
                            selected = it == currentDestination,
                            onClick = { currentDestination = it },
                            icon = { Icon(it.icon, contentDescription = it.description) },
                            label = { Text(it.label) }
                        )
                    }
                }
            ) {
                when (currentDestination) {
                    NavigationDestinations.HOME -> HomePage()
                }
            }
        }
    }
}

enum class NavigationDestinations(
    val label: String,
    val icon: ImageVector,
    val description: String
) {
    HOME("首页", Lucide.House, "首页")
}
