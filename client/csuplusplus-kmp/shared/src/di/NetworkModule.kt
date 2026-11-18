package ink.qingshuige.csuplusplus.di

import ink.qingshuige.csuplusplus.core.network.ApiClient
import ink.qingshuige.csuplusplus.core.network.NetworkConfig
import ink.qingshuige.csuplusplus.core.network.PersistentTokenProvider
import ink.qingshuige.csuplusplus.core.network.TokenProvider
import ink.qingshuige.csuplusplus.core.network.getOrCreateAuthKSafe
import ink.qingshuige.csuplusplus.data.api.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    single { NetworkConfig() }

    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }

    single<TokenProvider> { PersistentTokenProvider(getOrCreateAuthKSafe()) }

    single {
        val config: NetworkConfig = get()
        val json: Json = get()
        val tokenProvider: TokenProvider = get()

        HttpClient {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMillis
                requestTimeoutMillis = config.requestTimeoutMillis
                socketTimeoutMillis = config.socketTimeoutMillis
            }

            install(DefaultRequest) {
                url(config.normalizedBaseUrl)
                accept(ContentType.Application.Json)
            }

            if (config.isDebugLogging) {
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.INFO
                    sanitizeHeader { header -> header == HttpHeaders.Authorization }
                }
            }
        }.apply {
            plugin(HttpSend).intercept { request ->
                var sentBearerToken = false
                if (!request.headers.contains(HttpHeaders.Authorization)) {
                    tokenProvider.getToken()?.let { token ->
                        request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                        sentBearerToken = true
                    }
                }
                val call = execute(request)
                if (sentBearerToken && call.response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.onUnauthorized()
                }
                call
            }
        }
    }

    singleOf(::ApiClient)
    singleOf(::HealthApi)
    singleOf(::AuthApi)
    singleOf(::UniversityApi)
    singleOf(::CollegeApi)
    singleOf(::CourseApi)
    singleOf(::FileApi)
}

