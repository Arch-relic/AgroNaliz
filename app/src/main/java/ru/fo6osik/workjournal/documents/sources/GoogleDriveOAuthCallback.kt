package ru.fo6osik.workjournal.documents.sources

/**
 * Validated authorization response. This parser never exchanges or stores tokens.
 * Pass the callback's actual redirect URI (scheme, authority and path, without query/fragment).
 */
internal object GoogleDriveOAuthCallback {
    sealed class Result {
        data class Code(val authorizationCode: String) : Result()
        data class Denied(val error: String) : Result()
    }

    fun parse(
        pending: GoogleDriveOAuthPkce.PendingAuthorization,
        actualRedirectUri: String,
        query: String
    ): Result {
        pending.validateCallback(extractState(query), actualRedirectUri)
        val fields = parseQuery(query)
        val error = fields["error"]
        val code = fields["code"]
        require(error == null || code == null) { "Ambiguous OAuth callback" }
        if (error != null) {
            require(error.isNotBlank()) { "Empty OAuth error" }
            return Result.Denied(error)
        }
        require(!code.isNullOrBlank()) { "Missing OAuth authorization code" }
        return Result.Code(code)
    }

    private fun extractState(query: String): String? = parseQuery(query)["state"]

    private fun parseQuery(query: String): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        for (part in query.removePrefix("?").split('&')) {
            if (part.isEmpty()) continue
            val pair = part.split('=', limit = 2)
            val key = decode(pair[0])
            val value = decode(pair.getOrElse(1) { "" })
            require(key !in fields) { "Duplicate OAuth parameter" }
            fields[key] = value
        }
        return fields
    }

    private fun decode(encoded: String): String = java.net.URLDecoder.decode(encoded, "UTF-8")
}
