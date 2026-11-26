package ink.qingshuige.csuplusplus.di

import ink.qingshuige.csuplusplus.core.session.SessionStore
import ink.qingshuige.csuplusplus.getPlatform
import ink.qingshuige.csuplusplus.presentation.page.auth.AuthPageViewModel
import ink.qingshuige.csuplusplus.presentation.page.home.HomeViewModel
import ink.qingshuige.csuplusplus.presentation.page.me.MeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    includes(networkModule)
    single { getPlatform() }
    single { SessionStore(authApi = get(), tokenProvider = get()) }
    viewModelOf(::HomeViewModel)
    viewModelOf(::AuthPageViewModel)
    viewModelOf(::MeViewModel)
}
