// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import org.json.JSONObject
import java.util.UUID

/** A single in-app event to record via `Decisa.track`. */
class DecisaEvent private constructor(
    val eventId: String,
    val name: DecisaEventName,
    val value: Double?,
    val currency: String?,
    val customName: String?,
    val url: String?,
    val occurredAt: String?,
    val isTest: Boolean,
    val metadata: Map<String, Any>,
) {
    fun toTrackBody(
        visitorId: String,
        pixelKey: String,
        extraMetadata: Map<String, Any> = emptyMap(),
    ): JSONObject {
        val mergedMetadata = extraMetadata.toMutableMap()
        mergedMetadata.putAll(metadata)
        customName?.let { mergedMetadata["custom_event_name"] = it }

        val metadataJson = JSONObject()
        mergedMetadata.forEach { (key, value) -> metadataJson.put(key, value) }

        return JSONObject().apply {
            put("event_id", eventId)
            put("event_name", name.wireName)
            put("visitor_id", visitorId)
            put("pixel_key", pixelKey)
            put("is_test", isTest)
            put("metadata", metadataJson)
            value?.let { put("value", it) }
            currency?.let { put("currency", it) }
            url?.let { put("url", it) }
            occurredAt?.let { put("occurred_at", it) }
        }
    }

    companion object {
        private fun create(
            name: DecisaEventName,
            value: Double? = null,
            currency: String? = null,
            customName: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
            eventId: String? = null,
        ): DecisaEvent = DecisaEvent(
            eventId = eventId ?: "evt_${UUID.randomUUID()}",
            name = name,
            value = value,
            currency = currency,
            customName = customName,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun purchase(
            value: Double,
            currency: String,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.Purchase,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun lead(
            value: Double? = null,
            currency: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.Lead,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun completeRegistration(
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.CompleteRegistration,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun startTrial(
            value: Double? = null,
            currency: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.StartTrial,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun subscribe(
            value: Double? = null,
            currency: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.Subscribe,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun addToCart(
            value: Double? = null,
            currency: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.AddToCart,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun initiateCheckout(
            value: Double? = null,
            currency: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.InitiateCheckout,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun addPaymentInfo(
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.AddPaymentInfo,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun viewContent(
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.ViewContent,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun pageView(
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.PageView,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun search(
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.Search,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun appInstall(
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.AppInstall,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )

        fun custom(
            name: String,
            value: Double? = null,
            currency: String? = null,
            url: String? = null,
            occurredAt: String? = null,
            isTest: Boolean = false,
            metadata: Map<String, Any> = emptyMap(),
        ): DecisaEvent = create(
            name = DecisaEventName.Custom,
            customName = name,
            value = value,
            currency = currency,
            url = url,
            occurredAt = occurredAt,
            isTest = isTest,
            metadata = metadata,
        )
    }
}
