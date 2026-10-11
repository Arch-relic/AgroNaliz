package ru.fo6osik.workjournal.documents.sources

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

/** Supported Android authorization/Picker; no browser redirect, secret, or custom token grant. */
internal class GoogleDriveNativeAuthorization(context: Context) : DriveAuthorizationBackend {
    private val client = Identity.getAuthorizationClient(context.applicationContext)
    fun request(account: String, mimeTypes: String? = null): AuthorizationResult {
        val builder = AuthorizationRequest.builder()
            .setAccount(Account(account, "com.google"))
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .setOptOutIncludingGrantedScopes(true)
        if (mimeTypes != null) {
            builder.setPrompt(AuthorizationRequest.Prompt.CONSENT)
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER, "true")
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_ALLOW_MULTIPLE, "false")
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_ALLOW_FOLDER_SELECTION, "false")
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_MIMETYPES, mimeTypes)
        }
        return Tasks.await(client.authorize(builder.build()), 30, TimeUnit.SECONDS)
    }
    fun callback(data: Intent): AuthorizationResult = client.getAuthorizationResultFromIntent(data)
    fun grant(result: AuthorizationResult): DriveGrant {
        if (result.hasResolution()) throw DriveAuthorizationRequired()
        return DriveGrant(result.accessToken ?: throw DriveAuthorizationRequired(), result.grantedScopes.toSet(),
            result.tokenResponseParams?.getString("picked_file_ids"))
    }
    override fun authorize(account: String): DriveGrant = grant(request(account))
    override fun clearToken(token: String) {
        Tasks.await(client.clearToken(ClearTokenRequest.builder().setToken(token).build()), 30, TimeUnit.SECONDS)
    }
    override fun revoke(account: String) {
        Tasks.await(client.revokeAccess(RevokeAccessRequest.builder().setAccount(Account(account, "com.google"))
            .setScopes(listOf(Scope(DRIVE_FILE_SCOPE))).build()), 30, TimeUnit.SECONDS)
    }
}
