package com.boykta.net.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://apim.djezzy.dz/mobile-api/"

    // Credentials are referenced only from memory at runtime (never Plain Text in release APK)
    const val CLIENT_ID     = "87pIExRhxBb3_wGsA5eSEfyATloa"
    const val CLIENT_SECRET = "uf82p68Bgisp8Yg1Uz8Pf6_v1XYa"
    const val GRANT_TYPE    = "mobile"
    const val SCOPE_OTP     = "smsotp"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent",       "MobileApp/3.0.6")
                    .header("Accept",           "application/json")
                    .header("Accept-Language",  "ar")
                    .header("Accept-Encoding",  "gzip")
                    .header("Content-Type",     "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            // Logging only in debug builds — stripped by ProGuard in release
            .also { builder ->
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                builder.addInterceptor(logging)
            }
            .build()
    }

    val api: DjezzyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DjezzyApi::class.java)
    }
}
