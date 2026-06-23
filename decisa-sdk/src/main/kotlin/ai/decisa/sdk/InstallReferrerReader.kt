// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** The deferred-attribution signal read from the native platform on first launch. */
data class DeferredSignal(
    val platform: String? = null,
    val mclid: String? = null,
    val madid: String? = null,
) {
    companion object {
        val empty = DeferredSignal()
    }
}

internal interface InstallReferrerReading {
    suspend fun getDeferredSignal(): DeferredSignal
}

/** Reads the Play Install Referrer and extracts `dcs_mclid`. */
internal class InstallReferrerReader(
    private val context: Context,
) : InstallReferrerReading {
    private val mclidParam = "dcs_mclid"

    override suspend fun getDeferredSignal(): DeferredSignal {
        val mclid = readMclid()
        return DeferredSignal(
            platform = "android",
            mclid = mclid,
            madid = null,
        )
    }

    private suspend fun readMclid(): String? = suspendCancellableCoroutine { continuation ->
        val referrerClient = InstallReferrerClient.newBuilder(context.applicationContext).build()
        val mainHandler = Handler(Looper.getMainLooper())
        var replied = false

        fun reply(mclid: String?) {
            if (replied) return
            replied = true
            mainHandler.post {
                if (continuation.isActive) {
                    continuation.resume(mclid)
                }
            }
        }

        try {
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            val details: ReferrerDetails = referrerClient.installReferrer
                            reply(extractMclid(details.installReferrer))
                        } else {
                            reply(null)
                        }
                    } catch (_: Exception) {
                        reply(null)
                    } finally {
                        try {
                            referrerClient.endConnection()
                        } catch (_: Exception) {
                            // best-effort close
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    reply(null)
                }
            })
        } catch (_: Exception) {
            reply(null)
        }

        continuation.invokeOnCancellation {
            try {
                referrerClient.endConnection()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun extractMclid(referrer: String?): String? {
        if (referrer.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse("https://decisa.invalid/?$referrer")
            val value = uri.getQueryParameter(mclidParam)
            value?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
