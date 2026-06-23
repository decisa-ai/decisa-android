// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import android.content.Context
import androidx.annotation.VisibleForTesting

/**
 * The Decisa mobile attribution SDK entry point.
 *
 * Authenticates with the PUBLIC `pixel_key` (`dcs_px_...`) only — never a secret
 * `dcs_ak_` / `dcs_sk_` key, which must never ship in a mobile binary.
 */
object Decisa {
    private const val DEFAULT_BASE_URL = "https://api.decisa.ai"
    private const val PIXEL_KEY_PREFIX = "dcs_px_"

    @Volatile
    private var instance: DecisaClient? = null

    val attribution: DecisaAttribution?
        get() = instance?.attribution

    val isInitialized: Boolean
        get() = instance?.attribution != null

    /** Initializes the SDK. Idempotent within a process. */
    suspend fun initialize(
        context: Context,
        pixelKey: String,
        baseUrl: String = DEFAULT_BASE_URL,
    ) {
        if (instance != null) return

        check(pixelKey.startsWith(PIXEL_KEY_PREFIX)) {
            "Decisa: pixelKey must be a public pixel key (starts with \"$PIXEL_KEY_PREFIX\"). " +
                "Never ship a secret dcs_ak_ / dcs_sk_ key in a mobile binary."
        }

        val appContext = context.applicationContext
        val client = DecisaClient(
            pixelKey = pixelKey,
            transport = DecisaTransport(baseUrl),
            persistence = DecisaPersistence(appContext),
            referrerReader = InstallReferrerReader(appContext),
        )
        client.resolveOrRestore()

        if (instance == null) {
            instance = client
        }
    }

    /** Associates the current visitor with a known identity. */
    suspend fun identify(
        userId: String? = null,
        email: String? = null,
        phone: String? = null,
        firstName: String? = null,
        lastName: String? = null,
    ): Boolean {
        val client = instance ?: return false
        return client.identify(userId, email, phone, firstName, lastName)
    }

    /** Records an in-app [event] via `POST /v1/track`. */
    suspend fun track(event: DecisaEvent): Boolean {
        val client = instance ?: return false
        return client.track(event)
    }

    @VisibleForTesting
    internal suspend fun initializeForTesting(
        pixelKey: String,
        @Suppress("UNUSED_PARAMETER") baseUrl: String,
        transport: DecisaTransporting,
        persistence: DecisaPersisting,
        referrerReader: InstallReferrerReading,
    ) {
        if (instance != null) return

        val client = DecisaClient(
            pixelKey = pixelKey,
            transport = transport,
            persistence = persistence,
            referrerReader = referrerReader,
        )
        client.resolveOrRestore()

        if (instance == null) {
            instance = client
        }
    }

    @VisibleForTesting
    fun resetForTesting() {
        instance = null
    }
}
