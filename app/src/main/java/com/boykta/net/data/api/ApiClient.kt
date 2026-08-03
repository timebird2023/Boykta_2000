package com.boykta.net.data.api

import android.content.Context
import com.boykta.net.BuildConfig
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.TokenResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://apim.djezzy.dz/mobile-api/"

    const val CLIENT_ID     = "87pIExRhxBb3_wGsA5eSEfyATloa"
    const val CLIENT_SECRET = "uf82p68Bgisp8Yg1Uz8Pf6_v1XYa"
    const val GRANT_TYPE    = "mobile"
    const val SCOPE_OTP     = "smsotp"
    const val SCOPE_DJEZZY  = "djezzyAppV2"   // used for verifyOtp and token refresh

    /** Call from MainActivity.onCreate() before any API call is made. */
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    /**
     * OkHttp Authenticator — called automatically on 401 responses.
     * Uses the stored refresh_token to obtain a new access_token.
     * Runs synchronously inside OkHttp's network thread (runBlocking is acceptable here).
     */
    private val tokenRefreshAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
            // Don't retry if this is already a token endpoint request (prevents infinite loop)
            if (response.request.url.toString().contains("oauth2/token")) return null
            // Stop after 1 refresh attempt
            if (responseCount(response) >= 2) return null

            val ctx = appContext ?: return null

            val newToken = runBlocking {
                val storage = TokenStorage(ctx)
                val refreshToken = storage.refreshToken.firstOrNull() ?: return@runBlocking null
                try {
                    val body = FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .add("client_id", CLIENT_ID)
                        .add("client_secret", CLIENT_SECRET)
                        .add("scope", SCOPE_DJEZZY)
                        .build()
                    val req = Request.Builder()
                        .url("${BASE_URL}oauth2/token")
                        .post(body)
                        .header("User-Agent", "MobileApp/3.0.7")
                        .header("Accept", "application/json")
                        .build()
                    // Use a plain client (no authenticator) to avoid recursion
                    val plainClient = OkHttpClient()
                    val resp = plainClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val json = resp.body?.string() ?: return@runBlocking null
                        val tokenResp = Gson().fromJson(json, TokenResponse::class.java)
                        val newAccess = tokenResp.accessToken ?: return@runBlocking null
                        storage.updateToken(newAccess, tokenResp.refreshToken)
                        newAccess
                    } else null
                } catch (e: Exception) { null }
            } ?: return null

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }

        private fun responseCount(response: okhttp3.Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) { count++; prior = prior.priorResponse }
            return count
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .authenticator(tokenRefreshAuthenticator)
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent",      "MobileApp/3.0.7")
                    .header("Accept",          "application/json")
                    .header("Accept-Language", "ar")
                    .header("Accept-Encoding", "gzip")
                    // NOTE: Do NOT set Content-Type here — Retrofit sets it automatically:
                    //   @FormUrlEncoded → application/x-www-form-urlencoded
                    //   @Body (JSON)    → application/json
                    // Overriding it globally breaks the OAuth form-encoded endpoints.
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            // Logging only in debug builds — stripped by R8 in release
            .also { builder ->
                if (BuildConfig.DEBUG) {
                    val logging = HttpLoggingInterceptor()
                    logging.level = HttpLoggingInterceptor.Level.BODY
                    builder.addInterceptor(logging)
                }
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
