package ink.qingshuige.csuplusplus.di

import ink.qingshuige.csuplusplus.presentation.page.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    includes(networkModule)
    viewModelOf(::HomeViewModel)
}
