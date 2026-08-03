package com.boykta.net.data.api

import com.boykta.net.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface DjezzyApi {

    // ── Auth ─────────────────────────────────────────────────────────────────

    /**
     * Step 1 — request OTP.
     * IMPORTANT: msisdn/client_id/scope are QUERY PARAMS (not form-encoded body).
     * The JSON body is the consent agreement — always send it.
     * Confirmed from Python bots: params={msisdn, client_id, scope}, json=body.
     */
    @POST("oauth2/registration")
    suspend fun requestOtp(
        @Query("msisdn")    msisdn: String,
        @Query("client_id") clientId: String,
        @Query("scope")     scope: String,
        @Body body: OtpRegistrationBody
    ): Response<Void>

    /**
     * Step 2 — verify OTP and get access_token.
     * IMPORTANT: field name is 'mobileNumber' (not 'msisdn'), and scope='djezzyAppV2'.
     * Confirmed from Python: payload has mobileNumber + scope + client_id + client_secret + grant_type.
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun verifyOtp(
        @Field("otp")           otp: String,
        @Field("mobileNumber")  msisdn: String,
        @Field("scope")         scope: String,
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("grant_type")    grantType: String
    ): Response<TokenResponse>

    /**
     * Refresh an expired access_token using the stored refresh_token.
     * Confirmed from Python: grant_type=refresh_token, same endpoint as verifyOtp.
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refreshToken(
        @Field("grant_type")    grantType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("client_id")     clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("scope")         scope: String
    ): Response<TokenResponse>

    // ── Balance ───────────────────────────────────────────────────────────────

    @GET("api/v1/subscribers/main-balance/{msisdn}")
    suspend fun getMainBalance(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<MainBalanceData>>

    /**
     * Returns nested: { data: { products: [ { commercialName, expiryAt, balances: [] } ] } }
     * Old model (flat List<ProductBalance>) was wrong — use ConnectedProductsData wrapper.
     */
    @GET("api/v1/subscribers/connected-products-balances/{msisdn}")
    suspend fun getProductBalances(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<ConnectedProductsData>>

    @GET("api/v1/subscribers/subscription-history/{msisdn}")
    suspend fun getSubscriptionHistory(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<List<SubscriptionHistoryItem>>>

    // ── Activate offer ────────────────────────────────────────────────────────

    @POST("api/v1/subscribers/activate-product/{msisdn}")
    suspend fun activateProduct(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: ActivateProductRequest
    ): Response<ApiResponse<Any>>

    /** Shake-type offers: GET to verify offer is available (data.code == packageCode), then POST. */
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

    /** Check remaining free bip-sms quota (callMeRemaining, flexyLiRemaining). */
    @GET("api/v1/customer-care/bip-sms/{msisdn}")
    suspend fun getBipSmsBalance(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<BipSmsBalanceData>>

    // ── MGM ───────────────────────────────────────────────────────────────────

    @POST("api/v1/services/mgm/send-invitation/{msisdn}")
    suspend fun sendMgmInvitation(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: MgmInviteRequest
    ): Response<ApiResponse<Any>>

    @GET("api/v1/services/mgm/invitations/{msisdn}")
    suspend fun getMgmInvitations(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<MgmInvitationsData>>

    @POST("api/v1/services/mgm/activate-reward/{msisdn}")
    suspend fun activateMgmReward(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<Any>>

    // ── Network Services ─────────────────────────────────────────────────────
    // Confirmed from Reqable recordings:
    //   GET  api/v1/services/network-services/{msisdn}  → current states
    //   POST api/v1/services/network-services/{msisdn}  → body {"code":"APPELMASQUE","activate":bool}

    @GET("api/v1/services/network-services/{msisdn}")
    suspend fun getNetworkServices(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String
    ): Response<ApiResponse<List<NetworkServiceItem>>>

    @POST("api/v1/services/network-services/{msisdn}")
    suspend fun toggleNetworkService(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: NetworkServiceRequest
    ): Response<ApiResponse<Any>>

    // ── Ranati / RBT ring-back tone ───────────────────────────────────────────

    @GET("content/api/v1/subscribers/{msisdn}")
    suspend fun checkRanatiSubscription(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Query("include") include: String = "rbt-subscriptions"
    ): Response<ApiResponse<Any>>

    @HTTP(method = "DELETE", path = "content/api/v1/subscribers/{msisdn}", hasBody = true)
    suspend fun deleteRanati(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: RanatiDeleteBody
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
