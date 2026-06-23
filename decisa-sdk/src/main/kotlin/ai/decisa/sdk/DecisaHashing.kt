// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import java.security.MessageDigest

/** Client-side identity hashing for `/v1/identify`. Raw PII never leaves the device. */
internal object DecisaHashing {
    fun email(raw: String?): String? {
        val normalized = normalize(raw) ?: return null
        return sha256Hex(normalized)
    }

    fun phone(raw: String?): String? {
        if (raw == null) return null
        val stripped = raw.replace(Regex("[^0-9+]"), "")
        val normalized = normalize(stripped) ?: return null
        return sha256Hex(normalized)
    }

    fun text(raw: String?): String? {
        val normalized = normalize(raw) ?: return null
        return sha256Hex(normalized)
    }

    private fun normalize(raw: String?): String? {
        if (raw == null) return null
        val value = raw.trim().lowercase()
        return value.ifEmpty { null }
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
