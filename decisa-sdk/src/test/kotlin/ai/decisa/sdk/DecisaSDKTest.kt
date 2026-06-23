// Copyright (c) Decisa. MIT licensed. See LICENSE.

package ai.decisa.sdk

import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class MockTransport(
    private val handler: (String, JSONObject) -> DecisaResponse,
) : DecisaTransporting {
    val sentBodies = mutableListOf<JSONObject>()
    val sentPaths = mutableListOf<String>()

    override suspend fun post(path: String, body: JSONObject): DecisaResponse {
        sentPaths.add(path)
        sentBodies.add(body)
        return handler(path, body)
    }
}

private class MockPersistence : DecisaPersisting {
    private var resolved = false
    private var attribution: DecisaAttribution? = null
    private var externalId: String? = null

    override fun hasResolved(): Boolean = resolved

    override fun saveAttribution(attribution: DecisaAttribution) {
        this.attribution = attribution
        resolved = true
    }

    override fun readAttribution(): DecisaAttribution? = attribution

    override fun saveExternalId(externalId: String) {
        this.externalId = externalId
    }

    override fun readExternalId(): String? = externalId

    override fun clear() {
        resolved = false
        attribution = null
        externalId = null
    }
}

private class MockReferrerReader(
    private val signal: DeferredSignal,
) : InstallReferrerReading {
    override suspend fun getDeferredSignal(): DeferredSignal = signal
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class DecisaSDKTest {
    @Before
    fun setUp() {
        Decisa.resetForTesting()
    }

    @After
    fun tearDown() {
        Decisa.resetForTesting()
    }

    @Test
    fun initializeResolvesMatchedInstallAndPersistsVisitorId() = runTest {
        val transport = MockTransport { path, _ ->
            assertEquals("/v1/resolve", path)
            DecisaResponse(
                statusCode = 200,
                data = JSONObject().apply {
                    put("visitor_id", "v_server_123")
                    put("matched", true)
                    put("match_type", "mclid")
                    put("utm_source", "google")
                    put("utm_campaign", "spring_sale")
                },
            )
        }
        val persistence = MockPersistence()

        Decisa.initializeForTesting(
            pixelKey = "dcs_px_abc",
            baseUrl = "https://api.decisa.ai",
            transport = transport,
            persistence = persistence,
            referrerReader = MockReferrerReader(
                DeferredSignal(platform = "android", mclid = "tok_xyz"),
            ),
        )

        assertTrue(Decisa.isInitialized)
        assertEquals("v_server_123", Decisa.attribution?.visitorId)
        assertEquals(true, Decisa.attribution?.matched)
        assertEquals("spring_sale", Decisa.attribution?.utmCampaign)
        assertEquals("tok_xyz", transport.sentBodies.first().getString("mclid"))
        assertEquals("dcs_px_abc", transport.sentBodies.first().getString("pixel_key"))
        assertTrue(persistence.hasResolved())
    }

    @Test
    fun initializeFallsBackToLocalVisitorIdOn204() = runTest {
        val transport = MockTransport { _, _ ->
            DecisaResponse(statusCode = 204)
        }

        Decisa.initializeForTesting(
            pixelKey = "dcs_px_abc",
            baseUrl = "https://api.decisa.ai",
            transport = transport,
            persistence = MockPersistence(),
            referrerReader = MockReferrerReader(DeferredSignal.empty),
        )

        assertEquals(false, Decisa.attribution?.matched)
        assertTrue(Decisa.attribution?.visitorId?.startsWith("v_") == true)
    }

    @Test
    fun identifyHashesEmailClientSideAndNeverSendsRawPii() = runTest {
        var identifyBody: JSONObject? = null
        val transport = MockTransport { path, body ->
            if (path == "/v1/resolve") {
                DecisaResponse(
                    statusCode = 200,
                    data = JSONObject().apply {
                        put("visitor_id", "v_1")
                        put("matched", false)
                    },
                )
            } else {
                identifyBody = body
                DecisaResponse(statusCode = 202)
            }
        }

        Decisa.initializeForTesting(
            pixelKey = "dcs_px_abc",
            baseUrl = "https://api.decisa.ai",
            transport = transport,
            persistence = MockPersistence(),
            referrerReader = MockReferrerReader(DeferredSignal.empty),
        )

        val ok = Decisa.identify(
            userId = "user_42",
            email = "  Jane@Example.COM ",
        )

        assertTrue(ok)
        val expectedHash = sha256Hex("jane@example.com")
        assertEquals(expectedHash, identifyBody?.getString("email_sha256"))
        assertEquals("user_42", identifyBody?.getString("external_id"))

        val bodyString = identifyBody.toString()
        assertFalse(bodyString.contains("Jane@Example.COM"))
        assertFalse(bodyString.contains("jane@example.com"))
    }

    @Test
    fun trackSendsCanonicalEventNameValueAndUtmMetadata() = runTest {
        var trackBody: JSONObject? = null
        val transport = MockTransport { path, body ->
            if (path == "/v1/resolve") {
                DecisaResponse(
                    statusCode = 200,
                    data = JSONObject().apply {
                        put("visitor_id", "v_1")
                        put("matched", true)
                        put("utm_source", "meta")
                        put("utm_campaign", "launch")
                    },
                )
            } else {
                trackBody = body
                DecisaResponse(statusCode = 202)
            }
        }

        Decisa.initializeForTesting(
            pixelKey = "dcs_px_abc",
            baseUrl = "https://api.decisa.ai",
            transport = transport,
            persistence = MockPersistence(),
            referrerReader = MockReferrerReader(
                DeferredSignal(platform = "android", madid = "GAID-1234"),
            ),
        )

        val ok = Decisa.track(DecisaEvent.purchase(value = 49.90, currency = "USD"))

        assertTrue(ok)
        assertEquals("Purchase", trackBody?.getString("event_name"))
        assertEquals(49.90, trackBody?.getDouble("value")!!, 0.001)
        assertEquals("USD", trackBody?.getString("currency"))
        assertEquals("v_1", trackBody?.getString("visitor_id"))
        val eventId = trackBody?.getString("event_id")
        assertNotNull(eventId)
        assertTrue(eventId!!.length >= 8)

        val metadata = trackBody?.getJSONObject("metadata")
        assertEquals("meta", metadata?.getString("utm_source"))
        assertEquals("GAID-1234", metadata?.getString("madid"))
    }

    @Test
    fun customEventMapsToCustomWithLabelInMetadata() {
        val event = DecisaEvent.custom("viewed_pricing")
        val body = event.toTrackBody(visitorId = "v_1", pixelKey = "dcs_px_abc")

        assertEquals("Custom", body.getString("event_name"))
        assertEquals("viewed_pricing", body.getJSONObject("metadata").getString("custom_event_name"))
    }

    @Test
    fun initializeIsIdempotent() = runTest {
        var resolveCallCount = 0
        val transport = MockTransport { path, _ ->
            if (path == "/v1/resolve") {
                resolveCallCount++
                DecisaResponse(
                    statusCode = 200,
                    data = JSONObject().apply {
                        put("visitor_id", "v_1")
                        put("matched", false)
                    },
                )
            } else {
                DecisaResponse.networkFailure
            }
        }

        val persistence = MockPersistence()
        val referrerReader = MockReferrerReader(DeferredSignal.empty)

        Decisa.initializeForTesting(
            pixelKey = "dcs_px_abc",
            baseUrl = "https://api.decisa.ai",
            transport = transport,
            persistence = persistence,
            referrerReader = referrerReader,
        )

        Decisa.initializeForTesting(
            pixelKey = "dcs_px_abc",
            baseUrl = "https://api.decisa.ai",
            transport = transport,
            persistence = persistence,
            referrerReader = referrerReader,
        )

        assertEquals(1, resolveCallCount)
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
