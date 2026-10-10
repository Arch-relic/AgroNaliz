package ru.fo6osik.workjournal.documents.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Public-client OAuth token exchange via HTTPS, without a bundled client secret.
 * Does not store tokens or log responses. Network calls run on Dispatchers.IO.
 */
internal class GoogleDriveHttpTokenExchange(
    private val endpoint: String = "https://oauth2.googleapis.com/token"
) : GoogleDriveTokenExchange {
    init {
        require(endpoint.startsWith("https://")) { "Token endpoint must use HTTPS" }
    }

    suspend fun refresh(clientId: String, refreshToken: String): GoogleDriveTokenExchange.Tokens {
        require(refreshToken.isNotBlank()) { "Missing refresh token" }
        return requestTokens(linkedMapOf(
            "client_id" to clientId,
            "refresh_token" to refreshToken,
            "grant_type" to "refresh_token"
        ))
    }

    override suspend fun exchange(
        clientId: String,
        request: GoogleDriveOAuthAttempt.ExchangeRequest
    ): GoogleDriveTokenExchange.Tokens = requestTokens(linkedMapOf(
        "client_id" to clientId,
        "code" to request.code,
        "code_verifier" to request.codeVerifier,
        "redirect_uri" to request.redirectUri,
        "grant_type" to "authorization_code"
    ))

    private suspend fun requestTokens(parameters: Map<String, String>): GoogleDriveTokenExchange.Tokens = withContext(Dispatchers.IO) {
        require(!parameters["client_id"].isNullOrBlank()) { "Missing OAuth client ID" }
        val body = parameters.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }.toByteArray(Charsets.UTF_8)

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Accept", "application/json")
            connection.instanceFollowRedirects = false
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status != 200) throw IOException("OAuth token endpoint returned HTTP $status")
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                // Token responses are small; bound them to avoid unbounded reads.
                val chars = CharArray(8192)
                val count = it.read(chars)
                if (count < 0) throw IOException("Empty OAuth token response")
                val extra = it.read()
                if (extra != -1) throw IOException("Oversized OAuth token response")
                String(chars, 0, count)
            }
            val json = org.json.JSONObject(response)
            val accessToken = json.optString("access_token")
            val expiresIn = json.optLong("expires_in", -1)
            val refreshToken = if (json.has("refresh_token")) json.optString("refresh_token") else null
            GoogleDriveTokenExchange.Tokens(accessToken, expiresIn, refreshToken)
        } catch (error: org.json.JSONException) {
            throw IOException("Malformed OAuth token response", error)
        } finally {
            connection.disconnect()
        }
    }
}
