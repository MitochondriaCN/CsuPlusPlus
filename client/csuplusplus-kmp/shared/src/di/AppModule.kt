package ink.qingshuige.csuplusplus.di

import ink.qingshuige.csuplusplus.presentation.page.auth.AuthPageViewModel
import ink.qingshuige.csuplusplus.presentation.page.home.HomeViewModel
import ink.qingshuige.csuplusplus.presentation.page.me.MeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    includes(networkModule)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AuthPageViewModel)
    viewModelOf(::MeViewModel)
}
