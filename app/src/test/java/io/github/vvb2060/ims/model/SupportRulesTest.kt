package io.github.vvb2060.ims.model

import org.json.JSONObject
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupportRulesTest {
    @Test
    fun supportBaseUrlIsTrimmedAndOptional() {
        assertEquals("https://support.example.com", SupportRules.normalizeBaseUrl(" https://support.example.com/ "))
        assertNull(SupportRules.normalizeBaseUrl("  "))
    }

    @Test
    fun supportAmountMustBeValidPositiveMoney() {
        assertEquals("9.90", SupportRules.normalizeSupportAmount("9.90"))
        assertEquals("0.01", SupportRules.normalizeSupportAmount("0.01"))
        assertNull(SupportRules.normalizeSupportAmount(""))
        assertNull(SupportRules.normalizeSupportAmount("0"))
        assertNull(SupportRules.normalizeSupportAmount("."))
        assertNull(SupportRules.normalizeSupportAmount("9..9"))
        assertNull(SupportRules.normalizeSupportAmount("9.999"))
    }

    @Test
    fun supportUrlKeepsUnconsumedParamsWhenTemplateHasPlaceholders() {
        val url = SupportRules.buildUrlWithQueryParams(
            template = "https://pay.example.com/support?amount={amount}",
            params = linkedMapOf(
                "amount" to "9.90",
                "payer_name" to "张三",
                "payer_message" to "继续加油",
            ),
            aliases = mapOf(
                "name" to "payer_name",
                "message" to "payer_message",
            ),
        )

        assertEquals(
            "https://pay.example.com/support?amount=9.90&payer_name=%E5%BC%A0%E4%B8%89&payer_message=%E7%BB%A7%E7%BB%AD%E5%8A%A0%E6%B2%B9",
            url
        )
    }

    @Test
    fun dodopaySupportUrlIncludesReturnMode() {
        val url = SupportRules.buildUrlWithQueryParams(
            template = "https://pay.dodododo.org/support/app_test",
            params = linkedMapOf(
                "amount" to "0.01",
                "payer_name" to "匿名用户",
                "payer_message" to "继续维护",
                "return_mode" to "close",
                "return_label" to "返回 App",
            ),
        )

        assertEquals(
            "https://pay.dodododo.org/support/app_test?amount=0.01&payer_name=%E5%8C%BF%E5%90%8D%E7%94%A8%E6%88%B7&payer_message=%E7%BB%A7%E7%BB%AD%E7%BB%B4%E6%8A%A4&return_mode=close&return_label=%E8%BF%94%E5%9B%9E+App",
            url
        )
    }

    @Test
    fun dodopayPublicFeedParsesDisplayOnlySupportRecords() {
        val records = SupportRules.parseSupportRecords(
            JSONObject(
                """
                {
                  "items": [
                    {
                      "record_id": "rec_123",
                      "order_id": "should_not_be_used",
                      "merchant_order_id": "should_not_be_used",
                      "amount": "9.90",
                      "paid_at": "2026-06-19T21:22:50.550Z",
                      "channel": "WECHAT",
                      "payer_name": "小马哥",
                      "payer_contact": "tg:@hidden",
                      "payer_message": "能否完善支持双sim卡的开关"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val record = records.single()
        assertEquals("rec_123", record.id)
        assertEquals("9.90", record.amount)
        assertEquals("2026-06-19T21:22:50.550Z", record.paidAt)
        assertEquals("WECHAT", record.channel)
        assertEquals("小马哥", record.payerName)
        assertEquals("能否完善支持双sim卡的开关", record.payerMessage)
    }

    @Test
    fun supportRecordTimeUsesDeviceTimeZone() {
        assertEquals(
            "2026-06-20 05:22",
            SupportRules.formatIsoDateTimeForDisplay(
                value = "2026-06-19T21:22:50.550Z",
                zoneId = ZoneId.of("Asia/Shanghai"),
            )
        )
    }

    @Test
    fun onlyDodopayCheckoutCloseUrlClosesPaymentDialog() {
        assertTrue(SupportRules.isDodopayCheckoutCloseUrl("https://pay.dodododo.org/checkout/close?order_id=test"))
        assertTrue(SupportRules.isDodopayCheckoutCloseUrl("https://pay.dodododo.org/checkout/close#payment_proof=proof_${"a".repeat(64)}"))
        assertFalse(SupportRules.isDodopayCheckoutCloseUrl("https://pay.dodododo.org/pay/test"))
        assertFalse(SupportRules.isDodopayCheckoutCloseUrl("http://pay.dodododo.org/checkout/close"))
        assertFalse(SupportRules.isDodopayCheckoutCloseUrl("https://github.com/checkout/close"))
        assertFalse(SupportRules.isDodopayCheckoutCloseUrl("http://localhost:3000/checkout/close"))
    }

    @Test
    fun dodopayPaymentProofCanComeFromFragmentOrQuery() {
        val proof = "proof_${"a".repeat(64)}"

        assertEquals(
            proof,
            SupportRules.extractDodopayPaymentProof("https://pay.dodododo.org/checkout/close#payment_proof=$proof")
        )
        assertEquals(
            proof,
            SupportRules.extractDodopayPaymentProof("https://pay.dodododo.org/checkout/close?payment_proof=$proof")
        )
        assertNull(SupportRules.extractDodopayPaymentProof("https://pay.dodododo.org/checkout/close#payment_proof=bad"))
        assertNull(SupportRules.extractDodopayPaymentProof("https://pay.dodododo.org/checkout/close#payment_proof=%"))
        assertNull(SupportRules.extractDodopayPaymentProof("https://pay.dodododo.org/checkout/close?payment_proof=%"))
        assertNull(SupportRules.extractDodopayPaymentProof("https://pay.dodododo.org/pay/test#payment_proof=$proof"))
    }

    @Test
    fun validPaymentProofUnlocksAdsOnlyForSameClientAndEnoughAmount() {
        val proof = PaymentProofVerification(
            valid = true,
            appId = "app_test",
            proofKey = SupportRules.AD_FREE_PROOF_KEY,
            clientRef = "client_123",
            amount = "100.00",
            status = "paid",
            paidAt = "2026-06-21T00:00:00.000Z",
            channel = "WECHAT",
        )

        assertTrue(SupportRules.isAdFreePaymentProof(proof, "client_123", "app_test"))
        assertFalse(SupportRules.isAdFreePaymentProof(proof.copy(amount = "99.99"), "client_123", "app_test"))
        assertFalse(SupportRules.isAdFreePaymentProof(proof.copy(clientRef = "client_other"), "client_123", "app_test"))
        assertFalse(SupportRules.isAdFreePaymentProof(proof.copy(proofKey = "other"), "client_123", "app_test"))
        assertFalse(SupportRules.isAdFreePaymentProof(proof.copy(appId = "app_other"), "client_123", "app_test"))
        assertFalse(SupportRules.isAdFreePaymentProof(proof.copy(valid = false), "client_123", "app_test"))
    }

    @Test
    fun supportAppIdCanBeExtractedFromPublicSupportUrl() {
        assertEquals(
            "app_c5b4614bad018dbd",
            SupportRules.extractSupportAppId("https://pay.dodododo.org/support/app_c5b4614bad018dbd")
        )
        assertNull(SupportRules.extractSupportAppId("https://pay.dodododo.org/pay/order"))
    }

    @Test
    fun businessIntentPayloadUsesMuggleLeadsIntentType() {
        val params = SupportRules.buildBusinessIntentParams(
            sourceName = "Carrier IMS",
            sourceVersion = "review",
            intentType = BusinessIntentType.ADS,
            name = "Review",
            contact = "review@example.com",
            message = "review valid type check",
        )

        assertEquals("ads", params["intent_type"])
        assertEquals("广告位出租", params["intent_type_label"])
        assertFalse(params.values.contains("business"))
    }

    @Test
    fun muggleLeadsImageSlotPayloadParsesAsCommercialAd() {
        val ads = SupportRules.parseCommercialAds(
            JSONObject(
                """
                {
                  "version": 1,
                  "slots": [
                    {
                      "tab": "home",
                      "position": "right",
                      "enabled": true,
                      "image_url": "https://leads.example.com/ad.png",
                      "click_url": "https://partner.example.com",
                      "alt": "合作广告",
                      "title": "首页合作",
                      "width": "clamp(240px, 22vw, 420px)",
                      "max_height": "72vh",
                      "fit": "natural"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val ad = ads.single()
        assertEquals(AdPlacement.HOME_POPUP, ad.placement)
        assertEquals("首页合作", ad.title)
        assertEquals("https://leads.example.com/ad.png", ad.imageUrl)
        assertEquals("https://partner.example.com", ad.actionUrl)
        assertEquals("合作广告", ad.altText)
        assertEquals("natural", ad.imageFit)
    }

    @Test
    fun cooperationAndLegacySupportSlotsParseAsCooperationAds() {
        val ads = SupportRules.parseCommercialAds(
            JSONObject(
                """
                {
                  "slots": [
                    {
                      "slot": {"group_key": "cooperation", "position_key": "card"},
                      "ad": {"title": "合作页广告", "click_url": "https://partner.example.com"}
                    },
                    {
                      "slot": {"group_key": "support", "position_key": "card"},
                      "ad": {"title": "旧广告位兼容", "click_url": "https://legacy.example.com"}
                    },
                    {
                      "placement": "SUPPORT_CARD",
                      "title": "旧 placement 兼容",
                      "click_url": "https://placement.example.com"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        assertEquals(3, ads.size)
        assertTrue(ads.all { it.placement == AdPlacement.COOPERATION_CARD })
    }

    @Test
    fun dismissedHomeAdReturnsAfterConfiguredInterval() {
        val ad = CommercialAd(
            id = "home",
            title = "合作",
            body = "内容",
            actionLabel = "查看",
            actionUrl = "https://example.com",
            placement = AdPlacement.HOME_POPUP,
            intervalHours = 2,
        )
        val now = 10_000_000L
        val thirtyMinutesAgo = now - 30L * 60L * 1000L
        val threeHoursAgo = now - 3L * 60L * 60L * 1000L

        assertFalse(SupportRules.shouldShowHomeAd(ad, now, lastShownAtMillis = threeHoursAgo, dismissedAtMillis = thirtyMinutesAgo))
        assertTrue(SupportRules.shouldShowHomeAd(ad, now, lastShownAtMillis = threeHoursAgo, dismissedAtMillis = threeHoursAgo))
    }

    @Test
    fun homeAdWithoutConfiguredIntervalShowsEveryLaunch() {
        val ad = CommercialAd(
            id = "home",
            title = "合作",
            body = "内容",
            actionLabel = "查看",
            actionUrl = "https://example.com",
            placement = AdPlacement.HOME_POPUP,
        )
        val now = 10_000_000L

        assertTrue(SupportRules.shouldShowHomeAd(ad, now, lastShownAtMillis = now, dismissedAtMillis = now))
    }

    @Test
    fun backupRestoreRequiresConfirmationWhenSimMccMncDiffers() {
        val backup = ConfigBackupSnapshot(
            id = "backup",
            name = "SIM",
            createdAtMillis = 1L,
            subId = 1,
            simTitle = "SIM",
            mcc = "460",
            mnc = "01",
            countryIso = "cn",
            featureValues = emptyMap(),
            countryMccOverride = "310",
        )

        assertFalse(SupportRules.requiresBackupMismatchConfirmation(backup, currentMcc = "460", currentMnc = "01"))
        assertTrue(SupportRules.requiresBackupMismatchConfirmation(backup, currentMcc = "310", currentMnc = "260"))
    }

    @Test
    fun apnDraftMustHaveValidMccMncBeforeConfirmation() {
        val valid = ApnDraftConfig("Carrier", "internet", "default,supl,ims", "460", "01")
        val invalid = ApnDraftConfig("Carrier", "internet", "default,supl,ims", "460", "")

        assertNull(SupportRules.validateApnDraft(valid))
        assertNotNull(SupportRules.validateApnDraft(invalid))
    }
}
