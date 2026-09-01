package com.boykta.net.data.api

import android.content.Context
import com.boykta.net.BuildConfig
import com.boykta.net.data.local.TokenStorage
import com.boykta.net.data.models.TokenResponse
import com.google.gson.Gson
import kotlinx.coroutines.delay
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
     * Implements intelligent retry to handle weak connections and 429 rate limits without killing session.
     */
    private val tokenRefreshAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
            // Don't retry if this is already a token endpoint request (prevents infinite loop)
            if (response.request.url.toString().contains("oauth2/token")) return null

            // Stop after 3 attempts
            if (responseCount(response) >= 3) return null

            val ctx = appContext ?: return null
            val newToken = runBlocking {
                val storage = TokenStorage(ctx)
                val refreshToken = storage.refreshToken.firstOrNull() ?: return@runBlocking null

                var refreshedAccessToken: String? = null
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

                val plainClient = OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .build()

                // Retry up to 3 times for weak connection / 429 / 5xx
                for (attempt in 1..3) {
                    try {
                        val resp = plainClient.newCall(req).execute()
                        val code = resp.code
                        val respBody = resp.body?.string() ?: ""

                        if (resp.isSuccessful) {
                            val tokenResp = Gson().fromJson(respBody, TokenResponse::class.java)
                            val newAccess = tokenResp.accessToken
                            if (!newAccess.isNullOrBlank()) {
                                storage.updateToken(newAccess, tokenResp.refreshToken)
                                refreshedAccessToken = newAccess
                                break
                            }
                        } else if (code == 400 || (code == 401 && (respBody.contains("invalid_grant") || respBody.contains("invalid_token")))) {
                            // Refresh token is definitely revoked/expired (user logged in elsewhere or 30 days passed)
                            storage.clearToken()
                            SessionManager.notifySessionExpired()
                            return@runBlocking null
                        } else {
                            // 429 or 5xx temporary server error - wait a bit and retry
                            if (attempt < 3) {
                                delay(500L * attempt)
                            }
                        }
                    } catch (e: Exception) {
                        // Network timeout / connection glitch - retry before giving up
                        if (attempt < 3) {
                            delay(500L * attempt)
                        }
                    }
                }
                refreshedAccessToken
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
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
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
