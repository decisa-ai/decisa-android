package ai.decisa.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Outcome of a transport call. */
data class DecisaResponse(
    val statusCode: Int,
    val data: JSONObject? = null,
    val error: JSONObject? = null,
) {
    val isSuccess: Boolean get() = statusCode in 200..299
    val isNoContent: Boolean get() = statusCode == 204

    companion object {
        val networkFailure = DecisaResponse(statusCode = 0)
    }
}

internal interface DecisaTransporting {
    suspend fun post(path: String, body: JSONObject): DecisaResponse
}

/** Thin HTTP transport for the three public ingest endpoints. */
internal class DecisaTransport(
    baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
) : DecisaTransporting {
    private val baseUrl = baseUrl.trimEnd('/')
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun post(path: String, body: JSONObject): DecisaResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl$path")
                .post(body.toString().toRequestBody(jsonMediaType))
                .header("Accept", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val statusCode = response.code
                    val responseBody = response.body?.string().orEmpty()
                    if (statusCode == 204 || responseBody.isEmpty()) {
                        return@withContext DecisaResponse(statusCode = statusCode)
                    }
                    val decoded = JSONObject(responseBody)
                    DecisaResponse(
                        statusCode = statusCode,
                        data = decoded.optJSONObject("data"),
                        error = decoded.optJSONObject("error"),
                    )
                }
            } catch (_: Exception) {
                DecisaResponse.networkFailure
            }
        }

    companion object {
        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .build()
    }
}
