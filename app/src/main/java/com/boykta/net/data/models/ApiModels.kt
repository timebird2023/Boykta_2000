package com.boykta.net.data.models

import com.google.gson.annotations.SerializedName

// ── Auth ─────────────────────────────────────────────────────────────────────

/**
 * JSON body for POST /oauth2/registration.
 * Confirmed from Python bots: always sent alongside the query params.
 */
data class OtpRegistrationBody(
    @SerializedName("consent-agreement") val consentAgreement: List<ConsentAgreementItem> = listOf(
        ConsentAgreementItem()
    ),
    @SerializedName("is-consent") val isConsent: Boolean = true
)

data class ConsentAgreementItem(
    @SerializedName("marketing-notifications") val marketingNotifications: Boolean = false
)

data class TokenResponse(
    @SerializedName("access_token")  val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in")    val expiresIn: Long?
)

// ── Generic API wrapper ───────────────────────────────────────────────────────

data class ApiMessage(
    @SerializedName("ar") val ar: String?,
    @SerializedName("fr") val fr: String?
)

data class ApiResponse<T>(
    @SerializedName("status")  val status: Int?,
    @SerializedName("message") val message: Any?,
    @SerializedName("data")    val data: T?
)

// ── Balance ───────────────────────────────────────────────────────────────────

data class MainBalanceData(
    @SerializedName("mainBalance")            val mainBalance: Double?,
    @SerializedName("customerInformations")   val customerInfo: CustomerInfo?
)

data class CustomerInfo(
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName")  val lastName: String?,
    @SerializedName("msisdn")    val msisdn: String?
)

// ── Connected Products (real structure confirmed from Python analysis) ─────────
//
// API returns: { "data": { "products": [ { "commercialName": {ar, fr},
//                                          "expiryAt": "...",
//                                          "balances": [ { "remaining": N, "usageUnit": "MB" } ]
//                                        } ] } }
// The old flat model was wrong — it matched nothing in the real response.

/** Wrapper: data field of connected-products-balances response */
data class ConnectedProductsData(
    @SerializedName("products") val products: List<ProductBalance>?
)

data class ProductBalance(
    @SerializedName("commercialName") val commercialName: CommercialName?,
    @SerializedName("expiryAt")       val expiryAt: String?,
    @SerializedName("balances")       val balances: List<BalanceItem>?
)

data class BalanceItem(
    @SerializedName("remaining")  val remaining: Double?,
    @SerializedName("usageUnit")  val usageUnit: String?   // "MB", "GB", "Voice", etc.
) {
    /** Human-readable remaining with Western (Locale.US) digits, e.g. "2.00 GB" or "512 MB" */
    fun displayRemaining(): String {
        val rem = remaining ?: return "--"
        return when {
            usageUnit == "MB" && rem > 1024 ->
                String.format(java.util.Locale.US, "%.2f", rem / 1024) + " GB"
            else ->
                String.format(java.util.Locale.US, "%.0f", rem) + " ${usageUnit ?: ""}"
        }
    }
}

data class CommercialName(
    @SerializedName("ar") val ar: String?,
    @SerializedName("fr") val fr: String?
)

data class SubscriptionHistoryItem(
    @SerializedName("packageCode")    val packageCode: String?,
    @SerializedName("packageFee")     val packageFee: Double?,
    @SerializedName("commercialName") val commercialName: CommercialName?,
    @SerializedName("activationDate") val activationDate: String?
)

// ── Flexy ─────────────────────────────────────────────────────────────────────

data class FlexyHistoryItem(
    @SerializedName("msisdnBParty") val msisdnBParty: String?,
    @SerializedName("amount")       val amount: Any?,
    @SerializedName("date")         val date: String?,
    @SerializedName("type")         val type: String?
)

// ── Offers ────────────────────────────────────────────────────────────────────

data class ActivateProductRequest(
    @SerializedName("packageCode") val packageCode: String
)

// ── Flexy requests ────────────────────────────────────────────────────────────

data class FlexyTransferRequest(
    @SerializedName("msisdnBParty") val msisdnBParty: Long,
    @SerializedName("amount")       val amount: Double,
    @SerializedName("password")     val password: String
)

data class FlexyDataRequest(
    @SerializedName("msisdnBParty") val msisdnBParty: Long,
    @SerializedName("packageCode")  val packageCode: String
)

// ── Free SMS ──────────────────────────────────────────────────────────────────

data class BipSmsRequest(
    @SerializedName("msisdnReceiver") val msisdnReceiver: Long,
    @SerializedName("type")           val type: String    // "CALLME" | "FLEXYLI"
)

data class BipSmsBalanceData(
    @SerializedName("callMeRemaining")  val callMeRemaining: Int?,
    @SerializedName("flexyLiRemaining") val flexyLiRemaining: Int?
)

// ── MGM ───────────────────────────────────────────────────────────────────────

data class MgmInviteRequest(
    @SerializedName("msisdnReceiver") val msisdnReceiver: Long
)

/** Wrapper for GET /api/v1/services/mgm/invitations/{msisdn} response data */
data class MgmInvitationsData(
    @SerializedName("invitations") val invitations: List<MgmInvitationServerItem>?
)

data class MgmInvitationServerItem(
    @SerializedName("id")              val id: String?,
    @SerializedName("msisdnReceiver")  val msisdnReceiver: String?,
    @SerializedName("status")          val status: String?,   // "DONE", "PENDING", etc.
    @SerializedName("smsSendAt")       val smsSendAt: String?,
    @SerializedName("createdAt")       val createdAt: String?
)

// ── Network Services ─────────────────────────────────────────────────────────
//
// Confirmed from Reqable recordings: body is {"code":"APPELMASQUE","activate":true/false}
// NOT {"serviceId":...,"action":"ACTIVATE"} — that format is wrong.

data class NetworkServiceRequest(
    @SerializedName("code")     val code: String,
    @SerializedName("activate") val activate: Boolean
)

data class NetworkServiceItem(
    @SerializedName("code")    val code: String?,
    @SerializedName("status")  val status: String?    // "ACTIVE" | "INACTIVE" | etc.
)

// ── Ranati (RBT ring-back tone) ───────────────────────────────────────────────

data class RanatiDeleteBody(
    @SerializedName("data") val data: RanatiDeleteData
)

data class RanatiDeleteData(
    @SerializedName("type") val type: String = "rbt-subscriptions",
    @SerializedName("id")   val id: String
)

// ── Local model for Offers screen ────────────────────────────────────────────

data class PaidOffer(
    val id: String,
    val name: String,
    val amount: String,
    val price: String,
    val duration: String,
    val packageCode: String,
    val activationType: String   // "shake" | "activate-product"
)

val PAID_OFFERS = listOf(
    PaidOffer("1",  "عرض 70دج 4Go",      "4 جيجابايت",   "70",   "24 ساعة", "BTLINTSPEEDDAY2Go",          "shake"),
    PaidOffer("2",  "عرض 100دج 2Go",     "2 جيجابايت",   "100",  "24 ساعة", "DOVINTSPEEDDAY1GoPRE",       "activate-product"),
    PaidOffer("3",  "عرض 30دج 300Mo",    "300 ميجابايت", "30",   "24 ساعة", "DOVINTSPEEDDAY100MoPRE",     "activate-product"),
    PaidOffer("4",  "عرض 50دج 600Mo",    "600 ميجابايت", "50",   "24 ساعة", "DOVINTSPEEDDAY250MoPRE",     "activate-product"),
    PaidOffer("5",  "عرض 150دج 4Go",     "4 جيجابايت",   "150",  "7 أيام",  "DOVINTSPEEDWEEK2GoPRE",      "activate-product"),
    PaidOffer("6",  "عرض 300دج 10Go",    "10 جيجابايت",  "300",  "7 أيام",  "DOVINTSPEEDWEEK3GoPRE",      "activate-product"),
    PaidOffer("7",  "عرض 190دج 10Go",    "10 جيجابايت",  "190",  "72 ساعة", "BTL4GBDAY",                  "shake"),
    PaidOffer("8",  "عرض 70دج 3Go FB",   "3 جيجابايت",   "70",   "3 أيام",  "1GBFB3DAY",                  "shake"),
    PaidOffer("9",  "عرض 500دج 12Go",    "12 جيجابايت",  "500",  "شهر",     "DOVINTSPEEDMONTH6GoPRE",     "activate-product"),
    PaidOffer("10", "عرض 1000دج 30Go",   "30 جيجابايت",  "1000", "شهر",     "DOVINTSPEEDMONTH15GoPRE",    "activate-product"),
    PaidOffer("11", "عرض 1500دج 60Go",   "60 جيجابايت",  "1500", "شهر",     "DOVINTSPEEDMONTH30GoPRE",    "activate-product"),
    PaidOffer("12", "عرض 2000دج 100Go",  "100 جيجابايت", "2000", "30 يوم",  "DOVINTSPEEDMONTH100GoPRE5G", "activate-product"),
    PaidOffer("13", "عرض 4000دج 200Go",  "200 جيجابايت", "4000", "30 يوم",  "DOVINTSPEEDMONTH220GoPRE5G", "activate-product"),
    // ── من boykta.py ─────────────────────────────────────────────────────────
    PaidOffer("14", "عرض 90دج 5Go",     "5 جيجابايت",   "90",   "24 ساعة", "BTL500MBDAY",                "shake")
)
