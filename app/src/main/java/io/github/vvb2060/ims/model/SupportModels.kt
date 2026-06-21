package io.github.vvb2060.ims.model

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AdPlacement {
    HOME_POPUP,
    COOPERATION_CARD,
}

enum class BusinessIntentType(
    val wireValue: String,
    val label: String,
) {
    ADS("ads", "广告位出租"),
    DEVELOPMENT("development", "项目开发合作"),
    TOKEN_SUPPLY("token_supply", "AI 中转站 Token 批发供应"),
    OTHER("other", "其他");

    companion object {
        fun fromWireValue(value: String): BusinessIntentType {
            return entries.firstOrNull { it.wireValue == value } ?: ADS
        }
    }
}

data class CommercialAd(
    val id: String,
    val title: String,
    val body: String,
    val actionLabel: String,
    val actionUrl: String,
    val placement: AdPlacement,
    val enabled: Boolean = true,
    val intervalHours: Int = 0,
    val imageUrl: String = "",
    val altText: String = "",
    val imageFit: String = "natural",
)

data class NetworkExitStatus(
    val ip: String,
    val ipVersion: String,
    val country: String,
    val region: String,
    val city: String,
    val org: String,
    val risk: String,
    val googleReachable: Boolean?,
    val tiktokReachable: Boolean?,
    val captivePortalReachable: Boolean?,
)

data class SupportRecord(
    val id: String,
    val amount: String,
    val paidAt: String,
    val channel: String,
    val payerName: String,
    val payerMessage: String,
)

data class PaymentProofVerification(
    val valid: Boolean,
    val appId: String,
    val proofKey: String,
    val clientRef: String,
    val amount: String,
    val status: String,
    val paidAt: String,
    val channel: String,
)

enum class SupportPaymentChannel(val queryValue: String) {
    ALIPAY("ALIPAY"),
    WECHAT("WECHAT"),
}

data class ConfigBackupSnapshot(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
    val subId: Int,
    val simTitle: String,
    val mcc: String,
    val mnc: String,
    val countryIso: String,
    val featureValues: Map<Feature, FeatureValue>,
    val countryMccOverride: String = "",
)

data class ApnDraftConfig(
    val name: String,
    val apn: String,
    val type: String,
    val mcc: String,
    val mnc: String,
)

object SupportRules {
    const val AD_FREE_PROOF_KEY = "support_unlock"
    private val paymentProofPattern = Regex("""proof_[0-9a-f]{64}""")
    private val supportAmountPattern = Regex("""(?:0|[1-9]\d{0,5})(?:\.\d{1,2})?""")
    private val adFreeAmount = BigDecimal("100.00")

    fun normalizeBaseUrl(value: String): String? {
        return value.trim().trimEnd('/').takeIf { it.isNotBlank() }
    }

    fun normalizeSupportAmount(value: String): String? {
        val trimmed = value.trim()
        if (!supportAmountPattern.matches(trimmed)) return null
        val amount = runCatching { BigDecimal(trimmed) }.getOrNull() ?: return null
        if (amount <= BigDecimal.ZERO) return null
        return trimmed
    }

    fun buildUrlWithQueryParams(
        template: String,
        params: Map<String, String>,
        aliases: Map<String, String> = emptyMap(),
    ): String {
        var result = template.trim()
        val consumedKeys = linkedSetOf<String>()
        params.forEach { (key, value) ->
            val placeholder = "{$key}"
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, encodeQueryValue(value))
                consumedKeys += key
            }
        }
        aliases.forEach { (alias, key) ->
            val placeholder = "{$alias}"
            val value = params[key].orEmpty()
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, encodeQueryValue(value))
                consumedKeys += key
            }
        }
        val query = params
            .filterKeys { it !in consumedKeys }
            .filterValues { it.isNotBlank() }
            .entries
            .joinToString("&") { (key, value) ->
                "${encodeQueryValue(key)}=${encodeQueryValue(value)}"
            }
        if (query.isBlank()) return result
        val separator = when {
            result.endsWith("?") || result.endsWith("&") -> ""
            result.contains("?") -> "&"
            else -> "?"
        }
        return result + separator + query
    }

    fun isDodopayCheckoutCloseUrl(value: String): Boolean {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        val host = uri.host?.lowercase(Locale.US).orEmpty()
        val path = uri.path.orEmpty().trimEnd('/')
        return scheme == "https" &&
            host == "pay.dodododo.org" &&
            path == "/checkout/close"
    }

    fun extractDodopayPaymentProof(value: String): String? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        if (!isDodopayCheckoutCloseUrl(value)) return null
        val proof = findParamValue(uri.fragment, "payment_proof")
            ?: findParamValue(uri.query, "payment_proof")
            ?: return null
        return proof.takeIf { paymentProofPattern.matches(it) }
    }

    fun resolveUrlOrigin(value: String): String? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        val authority = uri.rawAuthority.orEmpty()
        if (scheme !in setOf("http", "https") || authority.isBlank()) return null
        return "$scheme://$authority"
    }

    fun extractSupportAppId(value: String): String? {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return null
        val segments = uri.path.orEmpty().trim('/').split('/')
        if (segments.size < 2 || segments[0] != "support") return null
        return segments[1].takeIf { it.isNotBlank() && !it.contains('{') && !it.contains('}') }
    }

    fun parsePaymentProofVerification(json: JSONObject): PaymentProofVerification {
        return PaymentProofVerification(
            valid = json.optBoolean("valid", false),
            appId = json.optString("app_id"),
            proofKey = json.optString("proof_key"),
            clientRef = json.optString("client_ref"),
            amount = json.optString("amount"),
            status = json.optString("status"),
            paidAt = json.optString("paid_at"),
            channel = json.optString("channel"),
        )
    }

    fun isAdFreePaymentProof(
        proof: PaymentProofVerification,
        expectedClientRef: String,
        expectedAppId: String?,
    ): Boolean {
        if (!proof.valid) return false
        if (expectedAppId.isNullOrBlank() || proof.appId != expectedAppId) return false
        if (proof.proofKey != AD_FREE_PROOF_KEY) return false
        if (proof.clientRef != expectedClientRef) return false
        if (proof.status != "paid") return false
        val amount = runCatching { BigDecimal(proof.amount) }.getOrNull() ?: return false
        return amount >= adFreeAmount
    }

    fun buildBusinessIntentParams(
        sourceName: String,
        sourceVersion: String,
        intentType: BusinessIntentType,
        name: String,
        contact: String,
        message: String,
    ): Map<String, String> {
        return linkedMapOf(
            "source_name" to sourceName,
            "source_version" to sourceVersion,
            "intent_type" to intentType.wireValue,
            "intent_type_label" to intentType.label,
            "name" to name.trim().ifBlank { "未填写" },
            "contact" to contact.trim(),
            "message" to message.trim(),
        )
    }

    fun parseCommercialAds(json: JSONObject): List<CommercialAd> {
        val items = json.optJSONArray("slots")
            ?: json.optJSONArray("items")
            ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                parseCommercialAdItem(item, index)?.let { add(it) }
            }
        }.filter { it.enabled && (it.title.isNotBlank() || it.imageUrl.isNotBlank()) }
    }

    fun parseSupportRecords(json: JSONObject): List<SupportRecord> {
        val items = json.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("record_id").ifBlank { "record_$index" }
                val amount = item.optString("amount")
                val paidAt = item.optString("paid_at")
                if (amount.isBlank() || paidAt.isBlank()) continue
                add(
                    SupportRecord(
                        id = id,
                        amount = amount,
                        paidAt = paidAt,
                        channel = item.optString("channel"),
                        payerName = item.optString("payer_name"),
                        payerMessage = item.optString("payer_message"),
                    )
                )
            }
        }
    }

    fun formatIsoDateTimeForDisplay(
        value: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        return runCatching {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale)
                .withZone(zoneId)
                .format(Instant.parse(value))
        }.getOrElse {
            value.replace('T', ' ').take(16)
        }
    }

    private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun decodeQueryValue(value: String): String {
        return URLDecoder.decode(value, Charsets.UTF_8.name())
    }

    private fun findParamValue(rawParams: String?, name: String): String? {
        if (rawParams.isNullOrBlank()) return null
        return rawParams
            .split('&')
            .asSequence()
            .mapNotNull { part ->
                val separatorIndex = part.indexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null
                val key = runCatching {
                    decodeQueryValue(part.substring(0, separatorIndex))
                }.getOrNull() ?: return@mapNotNull null
                val value = runCatching {
                    decodeQueryValue(part.substring(separatorIndex + 1))
                }.getOrNull() ?: return@mapNotNull null
                key to value
            }
            .firstOrNull { it.first == name }
            ?.second
    }

    private fun parseCommercialAdItem(item: JSONObject, index: Int): CommercialAd? {
        val slot = item.optJSONObject("slot") ?: item
        val creative = item.optJSONObject("ad")
            ?: item.optJSONObject("campaign")
            ?: item.optJSONObject("creative")
            ?: item.optJSONObject("content")
        val source = creative ?: item
        val imageUrl = firstNonBlank(
            source.optString("image_url"),
            source.optString("imageUrl"),
            source.optString("image"),
            item.optString("image_url"),
        )
        val actionUrl = firstNonBlank(
            source.optString("click_url"),
            source.optString("action_url"),
            source.optString("target_url"),
            source.optString("url"),
            source.optString("link"),
        )
        val altText = firstNonBlank(
            source.optString("alt"),
            source.optString("alt_text"),
            item.optString("alt"),
        )
        val hasCreativePayload = creative != null ||
            source.has("title") ||
            source.has("body") ||
            source.has("description") ||
            source.has("action_url") ||
            source.has("target_url") ||
            source.has("url") ||
            source.has("link") ||
            source.has("image_url") ||
            source.has("click_url")
        if (!hasCreativePayload) return null

        val title = firstNonBlank(
            source.optString("title"),
            source.optString("display_title"),
            source.optString("name"),
            item.optString("title"),
            altText,
        )
        val body = firstNonBlank(
            source.optString("body"),
            source.optString("description"),
            source.optString("summary"),
            if (title == altText) "" else altText,
        )
        return CommercialAd(
            id = firstNonBlank(
                source.optString("id"),
                item.optString("id"),
                stableAdId(index, imageUrl, actionUrl, title)
            ),
            title = title,
            body = body,
            actionLabel = firstNonBlank(
                source.optString("action_label"),
                source.optString("cta_label"),
                source.optString("button_text"),
                "查看"
            ),
            actionUrl = actionUrl,
            placement = resolveAdPlacement(
                rawPlacement = firstNonBlank(source.optString("placement"), item.optString("placement")),
                groupKey = firstNonBlank(
                    slot.optString("group_key"),
                    slot.optString("tab"),
                    item.optString("group_key"),
                    item.optString("tab"),
                ),
                positionKey = firstNonBlank(
                    slot.optString("position_key"),
                    slot.optString("position"),
                    item.optString("position_key"),
                    item.optString("position"),
                ),
            ),
            enabled = item.optBoolean("enabled", true) && source.optBoolean("enabled", true),
            intervalHours = source.optInt("interval_hours", item.optInt("interval_hours", 0)),
            imageUrl = imageUrl,
            altText = altText,
            imageFit = firstNonBlank(
                source.optString("fit"),
                item.optString("fit"),
                "natural",
            ),
        )
    }

    private fun resolveAdPlacement(
        rawPlacement: String,
        groupKey: String,
        positionKey: String,
    ): AdPlacement {
        when (rawPlacement.trim().uppercase(Locale.US)) {
            "HOME_POPUP" -> return AdPlacement.HOME_POPUP
            "SUPPORT_CARD", "COOPERATION_CARD" -> return AdPlacement.COOPERATION_CARD
        }
        val group = groupKey.lowercase(Locale.US)
        val position = positionKey.lowercase(Locale.US)
        return when {
            group == "home" -> AdPlacement.HOME_POPUP
            group in setOf("cooperation", "support") && position in setOf("card", "inline", "banner") -> {
                AdPlacement.COOPERATION_CARD
            }
            else -> AdPlacement.COOPERATION_CARD
        }
    }

    private fun stableAdId(
        index: Int,
        imageUrl: String,
        actionUrl: String,
        title: String,
    ): String {
        val key = firstNonBlank(imageUrl, actionUrl, title, "ad_$index")
        return "ad_${index}_${Integer.toHexString(key.hashCode())}"
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    fun shouldShowHomeAd(
        ad: CommercialAd,
        nowMillis: Long,
        lastShownAtMillis: Long,
        dismissedAtMillis: Long,
    ): Boolean {
        if (!ad.enabled || ad.placement != AdPlacement.HOME_POPUP) return false
        if (ad.intervalHours <= 0) return true
        val intervalMs = ad.intervalHours.coerceAtLeast(1) * 60L * 60L * 1000L
        if (dismissedAtMillis > 0L && nowMillis - dismissedAtMillis < intervalMs) return false
        return nowMillis - lastShownAtMillis >= intervalMs
    }

    fun requiresBackupMismatchConfirmation(
        backup: ConfigBackupSnapshot,
        currentMcc: String,
        currentMnc: String,
    ): Boolean {
        val backupMcc = normalizeMcc(backup.mcc)
        val backupMnc = normalizeMnc(backup.mnc)
        val selectedMcc = normalizeMcc(currentMcc)
        val selectedMnc = normalizeMnc(currentMnc)
        return backupMcc.isBlank() ||
            backupMnc.isBlank() ||
            selectedMcc.isBlank() ||
            selectedMnc.isBlank() ||
            backupMcc != selectedMcc ||
            backupMnc != selectedMnc
    }

    fun validateApnDraft(config: ApnDraftConfig): String? {
        val mcc = normalizeMcc(config.mcc)
        val mnc = normalizeMnc(config.mnc)
        return when {
            config.name.isBlank() -> "APN name is blank"
            config.apn.isBlank() -> "APN is blank"
            config.type.isBlank() -> "APN type is blank"
            mcc.length != 3 -> "MCC must be 3 digits"
            mnc.length !in 2..3 -> "MNC must be 2 or 3 digits"
            else -> null
        }
    }

    fun normalizeMcc(value: String): String {
        return value.filter { it.isDigit() }.take(3)
    }

    fun normalizeMnc(value: String): String {
        val digits = value.filter { it.isDigit() }.take(3)
        return if (digits.length == 1) digits.padStart(2, '0') else digits
    }
}
