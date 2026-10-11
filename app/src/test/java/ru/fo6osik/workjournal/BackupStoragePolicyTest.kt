package ru.fo6osik.workjournal

import org.junit.Assert.fail
import org.junit.Test

class BackupStoragePolicyTest {
    private val testId = BackupStoragePolicy.TEST_APPLICATION_ID

    @Test fun testCanWriteOnlyItsOwnProvider() {
        BackupStoragePolicy.requireSafeWrite(testId, "content", "$testId.testbackups")
    }

    @Test fun testRejectsMainProviderAndSharedSafEvenWithUriGrant() {
        for (authority in listOf("ru.fo6osik.workjournal.testbackups", "com.android.externalstorage.documents", null)) {
            rejected { BackupStoragePolicy.requireSafeWrite(testId, "content", authority) }
        }
    }

    @Test fun testRejectsFileUrisAndMissingScheme() {
        for (scheme in listOf("file", null, "https")) {
            rejected { BackupStoragePolicy.requireSafeWrite(testId, scheme, "$testId.testbackups") }
        }
    }

    @Test fun mainKeepsExistingSafExportBehavior() {
        BackupStoragePolicy.requireSafeWrite("ru.fo6osik.workjournal", "content", "com.android.externalstorage.documents")
    }

    private fun rejected(block: () -> Unit) {
        try { block(); fail("Unsafe TEST destination accepted") } catch (_: IllegalArgumentException) { }
    }
}
