// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import org.json.JSONObject
import java.security.SecureRandom

internal class DecisaClient(
    private val pixelKey: String,
    private val transport: DecisaTransporting,
    private val persistence: DecisaPersisting,
    private val referrerReader: InstallReferrerReading,
) {
    var attribution: DecisaAttribution? = null
        private set

    private var madid: String? = null

    suspend fun resolveOrRestore() {
        val signal = referrerReader.getDeferredSignal()
        madid = signal.madid

        if (persistence.hasResolved()) {
            attribution = persistence.readAttribution()
                ?: DecisaAttribution.unmatched(generateFallbackVisitorId())
            return
        }

        attribution = resolve(signal)
        attribution?.let { persistence.saveAttribution(it) }
    }

    suspend fun resolve(signal: DeferredSignal): DecisaAttribution {
        val fallbackVisitorId = generateFallbackVisitorId()

        val body = JSONObject().apply {
            put("pixel_key", pixelKey)
            signal.mclid?.let { put("mclid", it) }
        }

        val response = transport.post("/v1/resolve", body)

        if (!response.isSuccess || response.isNoContent || response.data == null) {
            return DecisaAttribution.unmatched(fallbackVisitorId)
        }

        return DecisaAttribution.fromResolveData(response.data, fallbackVisitorId)
    }

    suspend fun identify(
        userId: String?,
        email: String?,
        phone: String?,
        firstName: String?,
        lastName: String?,
    ): Boolean {
        val visitorId = attribution?.visitorId ?: return false

        val trimmedUserId = userId?.trim().orEmpty()
        if (trimmedUserId.isNotEmpty()) {
            persistence.saveExternalId(trimmedUserId)
        }

        val externalId = trimmedUserId.takeIf { it.isNotEmpty() }
            ?: persistence.readExternalId()

        val emailHash = DecisaHashing.email(email)
        val phoneHash = DecisaHashing.phone(phone)
        val fnHash = DecisaHashing.text(firstName)
        val lnHash = DecisaHashing.text(lastName)

        val hasIdentity = emailHash != null ||
            phoneHash != null ||
            fnHash != null ||
            lnHash != null ||
            !externalId.isNullOrEmpty()

        if (!hasIdentity) return false

        val body = JSONObject().apply {
            put("pixel_key", pixelKey)
            put("visitor_id", visitorId)
            emailHash?.let { put("email_sha256", it) }
            phoneHash?.let { put("phone_sha256", it) }
            fnHash?.let { put("fn_sha256", it) }
            lnHash?.let { put("ln_sha256", it) }
            if (!externalId.isNullOrEmpty()) put("external_id", externalId)
        }

        val response = transport.post("/v1/identify", body)
        return response.isSuccess
    }

    suspend fun track(event: DecisaEvent): Boolean {
        val currentAttribution = attribution ?: return false

        val extraMetadata = currentAttribution.utmMap().toMutableMap()
        madid?.let { extraMetadata["madid"] = it }
        persistence.readExternalId()?.takeIf { it.isNotEmpty() }?.let {
            extraMetadata["external_id"] = it
        }

        val body = event.toTrackBody(
            visitorId = currentAttribution.visitorId,
            pixelKey = pixelKey,
            extraMetadata = extraMetadata,
        )

        val response = transport.post("/v1/track", body)
        return response.isSuccess
    }

    fun generateFallbackVisitorId(): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val buffer = StringBuilder("v_")
        repeat(24) {
            buffer.append(alphabet[random.nextInt(alphabet.length)])
        }
        return buffer.toString()
    }
}
