package com.sharemeal.app.core.network

import com.sharemeal.app.core.security.SessionTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: SessionTokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.currentAccessToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
