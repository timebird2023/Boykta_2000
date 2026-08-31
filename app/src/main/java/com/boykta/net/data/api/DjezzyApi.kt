package com.boykta.net.data.api

import com.boykta.net.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface DjezzyApi {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("oauth2/registration")
    suspend fun requestOtp(
        @Query("msisdn")    msisdn: String,
        @Query("client_id") clientId: String,
        @Query("scope")     scope: String,
        @Body body: OtpRegistrationBody
    ): Response<ApiResponse<Any>>

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

    // ── SIM Migration (تحويل نوع الشريحة) ─────────────────────────────────────
    @GET("api/v1/customer-care/migrations/{msisdn}")
    suspend fun getMigrationOptions(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Query("application") application: String = "MOBILEAPP"
    ): Response<ApiResponse<List<MigrationOptionItem>>>

    @POST("api/v1/customer-care/migrates/{msisdn}")
    suspend fun executeMigration(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: MigrationExecuteRequest
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

    // ── Ranati / RBT ──────────────────────────────────────────────────────────
    @GET("content/api/v1/subscribers/{msisdn}")
    suspend fun checkRanatiSubscription(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Query("include") include: String = "rbt-subscriptions"
    ): Response<ApiResponse<RanatiSubscriberData>>

    @POST("content/api/v1/subscribers/{msisdn}/relationships/rbt-subscriptions")
    suspend fun subscribeRanati(
        @Header("Authorization") auth: String,
        @Path("msisdn") msisdn: String,
        @Body body: RanatiActivateBody
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
