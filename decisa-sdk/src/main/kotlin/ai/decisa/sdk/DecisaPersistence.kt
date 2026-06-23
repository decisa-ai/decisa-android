// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import android.content.Context
import org.json.JSONObject

internal interface DecisaPersisting {
    fun hasResolved(): Boolean
    fun saveAttribution(attribution: DecisaAttribution)
    fun readAttribution(): DecisaAttribution?
    fun saveExternalId(externalId: String)
    fun readExternalId(): String?
    fun clear()
}

/** Device-local persistence for resolved attribution and identity. */
internal class DecisaPersistence(context: Context) : DecisaPersisting {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasResolved(): Boolean = prefs.getBoolean(KEY_RESOLVED, false)

    override fun saveAttribution(attribution: DecisaAttribution) {
        prefs.edit()
            .putString(KEY_ATTRIBUTION, attribution.toJson().toString())
            .putBoolean(KEY_RESOLVED, true)
            .apply()
    }

    override fun readAttribution(): DecisaAttribution? {
        val raw = prefs.getString(KEY_ATTRIBUTION, null) ?: return null
        if (raw.isEmpty()) return null
        return try {
            DecisaAttribution.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }

    override fun saveExternalId(externalId: String) {
        prefs.edit().putString(KEY_EXTERNAL_ID, externalId).apply()
    }

    override fun readExternalId(): String? = prefs.getString(KEY_EXTERNAL_ID, null)

    override fun clear() {
        prefs.edit()
            .remove(KEY_ATTRIBUTION)
            .remove(KEY_EXTERNAL_ID)
            .remove(KEY_RESOLVED)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "decisa_sdk"
        private const val KEY_ATTRIBUTION = "decisa.attribution"
        private const val KEY_EXTERNAL_ID = "decisa.external_id"
        private const val KEY_RESOLVED = "decisa.resolved"
    }
}
