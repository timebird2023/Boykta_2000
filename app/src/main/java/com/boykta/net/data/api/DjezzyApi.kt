package com.boykta.net.data.api

import com.boykta.net.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface DjezzyApi {

    // ── Auth ─────────────────────────────────────────────────────────────────

    /** Step 1 — request OTP (form-encoded) */
    @FormUrlEncoded
    @POST("oauth2/registration")
    suspend fun requestOtp(
        @Field("msisdn")    msisdn: String,
        @Field("client_id") clientId: String,
        @Field("scope")     scope: String
    ): Response<Void>

    /** Step 2 — verify OTP and obtain access_token */
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun verifyOtp(
        @Field("otp")           otp: String,
        @Field("msisdn")        msisdn: String,
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type")    grantType: String
    ): Response<TokenResponse>

    // ── Balance ───────────────────────────────────────────────────────────────

    @GET("api/v1/subscribers/main-balance/{msisdn}")
    suspend fun getMainBalance(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<MainBalanceData>>

    @GET("api/v1/subscribers/connected-products-balances/{msisdn}")
    suspend fun getProductBalances(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<List<ProductBalance>>>

    @GET("api/v1/subscribers/subscription-history/{msisdn}")
    suspend fun getSubscriptionHistory(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<List<SubscriptionHistoryItem>>>

    // ── Activate offer (standard) ─────────────────────────────────────────────

    @POST("api/v1/subscribers/activate-product/{msisdn}")
    suspend fun activateProduct(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: ActivateProductRequest
    ): Response<ApiResponse<Any>>

    /** Shake-type offers: GET first (checks eligibility), then POST */
    @GET("api/v1/services/shake/{msisdn}")
    suspend fun checkShake(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<Any>>

    @POST("api/v1/services/shake/{msisdn}")
    suspend fun activateShake(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: ActivateProductRequest
    ): Response<ApiResponse<Any>>

    // ── Flexy ─────────────────────────────────────────────────────────────────

    @POST("api/v1/services/flexy/credit/{msisdn}")
    suspend fun transferCredit(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: FlexyTransferRequest
    ): Response<ApiResponse<Any>>

    @GET("api/v1/services/flexy/history/{msisdn}")
    suspend fun getFlexyHistory(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Query("type") type: String    // "CREDIT_SHARE" | "DATA_SHARE"
    ): Response<ApiResponse<List<FlexyHistoryItem>>>

    @POST("api/v1/services/flexy/data/{msisdn}")
    suspend fun transferData(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: FlexyDataRequest
    ): Response<ApiResponse<Any>>

    // ── Free SMS ──────────────────────────────────────────────────────────────

    @POST("api/v1/customer-care/bip-sms/{msisdn}")
    suspend fun sendBipSms(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: BipSmsRequest
    ): Response<ApiResponse<Any>>

    // ── MGM ───────────────────────────────────────────────────────────────────

    @POST("api/v1/services/mgm/send-invitation/{msisdn}")
    suspend fun sendMgmInvitation(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: MgmInviteRequest
    ): Response<ApiResponse<Any>>

    @POST("api/v1/services/mgm/activate-reward/{msisdn}")
    suspend fun activateMgmReward(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<Any>>

    // ── Walk & Win ────────────────────────────────────────────────────────────

    @GET("api/v1/services/walk/campaign/{msisdn}")
    suspend fun checkWalkCampaign(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<Any>>

    @POST("api/v1/services/walk/activate-reward/{msisdn}")
    suspend fun activateWalkReward(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: ActivateProductRequest
    ): Response<ApiResponse<Any>>
}
