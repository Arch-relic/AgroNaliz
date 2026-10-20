package ru.fo6osik.workjournal

import org.junit.Assert.*
import org.junit.Test

class InstalledAppDiagnosticsTest {
    @Test fun sha256HasFullStableUppercaseFingerprint() {
        assertEquals(listOf("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD"),
            InstalledAppDiagnostics.fingerprints(listOf("abc".toByteArray())))
    }
    @Test fun multipleSignersArePreservedAndDuplicatesRemoved() {
        val a = byteArrayOf(1); val b = byteArrayOf(2)
        assertEquals(InstalledAppDiagnostics.fingerprints(listOf(a, b)), InstalledAppDiagnostics.fingerprints(listOf(b, a, a)))
        assertEquals(2, InstalledAppDiagnostics.fingerprints(listOf(a, b)).size)
    }
    @Test fun absentCertificateIsUnknownRatherThanAFabricatedPin() {
        assertTrue(InstalledAppDiagnostics.fingerprints(emptyList()).isEmpty())
    }
}
