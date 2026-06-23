// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import org.json.JSONObject

/** The deferred-attribution result resolved on first launch. */
data class DecisaAttribution(
    val visitorId: String,
    val matched: Boolean,
    val matchType: String? = null,
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmContent: String? = null,
    val utmTerm: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("visitor_id", visitorId)
        put("matched", matched)
        matchType?.let { put("match_type", it) }
        utmSource?.let { put("utm_source", it) }
        utmMedium?.let { put("utm_medium", it) }
        utmCampaign?.let { put("utm_campaign", it) }
        utmContent?.let { put("utm_content", it) }
        utmTerm?.let { put("utm_term", it) }
    }

    fun utmMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        utmSource?.let { map["utm_source"] = it }
        utmMedium?.let { map["utm_medium"] = it }
        utmCampaign?.let { map["utm_campaign"] = it }
        utmContent?.let { map["utm_content"] = it }
        utmTerm?.let { map["utm_term"] = it }
        return map
    }

    companion object {
        fun fromResolveData(data: JSONObject, fallbackVisitorId: String): DecisaAttribution {
            val rawVisitorId = data.optString("visitor_id", "").trim()
            val visitorId = if (rawVisitorId.isNotEmpty()) rawVisitorId else fallbackVisitorId
            return DecisaAttribution(
                visitorId = visitorId,
                matched = data.optBoolean("matched", false),
                matchType = data.optString("match_type").takeIf { it.isNotEmpty() },
                utmSource = data.optString("utm_source").takeIf { it.isNotEmpty() },
                utmMedium = data.optString("utm_medium").takeIf { it.isNotEmpty() },
                utmCampaign = data.optString("utm_campaign").takeIf { it.isNotEmpty() },
                utmContent = data.optString("utm_content").takeIf { it.isNotEmpty() },
                utmTerm = data.optString("utm_term").takeIf { it.isNotEmpty() },
            )
        }

        fun unmatched(fallbackVisitorId: String): DecisaAttribution =
            DecisaAttribution(visitorId = fallbackVisitorId, matched = false)

        fun fromJson(json: JSONObject): DecisaAttribution = DecisaAttribution(
            visitorId = json.getString("visitor_id"),
            matched = json.optBoolean("matched", false),
            matchType = json.optString("match_type").takeIf { it.isNotEmpty() },
            utmSource = json.optString("utm_source").takeIf { it.isNotEmpty() },
            utmMedium = json.optString("utm_medium").takeIf { it.isNotEmpty() },
            utmCampaign = json.optString("utm_campaign").takeIf { it.isNotEmpty() },
            utmContent = json.optString("utm_content").takeIf { it.isNotEmpty() },
            utmTerm = json.optString("utm_term").takeIf { it.isNotEmpty() },
        )
    }
}
