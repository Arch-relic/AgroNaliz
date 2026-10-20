package ru.fo6osik.workjournal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Public metadata of this installed package only. No keystore, network or database access. */
data class InstalledAppDiagnostics(
    val versionName: String?,
    val versionCode: Long?,
    val certificateSha256: List<String>
) {
    companion object {
        fun fingerprints(certificates: List<ByteArray>): List<String> = certificates.map { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        }.distinct().sorted()

        @Suppress("DEPRECATION")
        fun read(context: Context): InstalledAppDiagnostics {
            val manager = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            val info = try { manager.getPackageInfo(context.packageName, flags) }
                catch (_: Exception) {
                    try { manager.getPackageInfo(context.packageName, 0) }
                    catch (_: Exception) { return InstalledAppDiagnostics(null, null, emptyList()) }
                }
            val certificates = try {
                val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
                fingerprints(signatures.orEmpty().map { it.toByteArray() })
            } catch (_: Exception) { emptyList() }
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            return InstalledAppDiagnostics(info.versionName, code, certificates)
        }
    }
}
