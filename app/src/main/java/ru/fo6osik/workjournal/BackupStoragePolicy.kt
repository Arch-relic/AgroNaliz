package ru.fo6osik.workjournal

/** TEST must never write to a shared SAF destination, even if a grant exists. */
internal object BackupStoragePolicy {
    const val TEST_APPLICATION_ID = "ru.fo6osik.workjournal.debugtest"

    fun isTest(applicationId: String) = applicationId == TEST_APPLICATION_ID

    fun requireSafeWrite(applicationId: String, scheme: String?, authority: String?) {
        if (isTest(applicationId)) {
            require(scheme == "content" && authority == "$applicationId.testbackups") {
                "TEST backups can only be written to the private TEST backup provider"
            }
        }
    }
}
