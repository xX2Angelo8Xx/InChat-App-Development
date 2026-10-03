package com.inchat.localmedia

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Tests the final release archive, rather than only installing a debug build through adb. */
@RunWith(AndroidJUnit4::class)
class ReleaseArchiveTest {
    @Suppress("DEPRECATION")
    @Test fun androidCanReadReleaseNameIconAndBothSignatureApis() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val release = File(context.cacheDir, "release-archive.apk")
        // /data/local/tmp is shell-owned; keep the package parser itself in the app context.
        val fixture = instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/local-media-release.apk")
        ParcelFileDescriptor.AutoCloseInputStream(fixture).use { input ->
            release.outputStream().use { output -> input.copyTo(output) }
        }
        assertTrue("CI must push the complete release APK before instrumentation", release.length() > 100_000_000)
        try {
            val pm = context.packageManager
            val modern = pm.getPackageArchiveInfo(release.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            assertNotNull("Android could not parse the release archive", modern)
            val info = requireNotNull(modern)
            assertEquals("com.inchat.localmedia", info.packageName)
            assertEquals("0.2.0", info.versionName)
            assertEquals(3L, info.longVersionCode)
            val application = requireNotNull(info.applicationInfo)
            application.sourceDir = release.absolutePath
            application.publicSourceDir = release.absolutePath
            assertEquals("Local Media", pm.getApplicationLabel(application).toString())
            assertNotNull(pm.getApplicationIcon(application))
            val signing = requireNotNull(info.signingInfo)
            val cert = signing.apkContentsSigners.single().toByteArray()
            val sha256 = MessageDigest.getInstance("SHA-256").digest(cert).joinToString("") { "%02x".format(it) }
            assertEquals("b3f623aefee6270bdd08f22ffeb43ebc513e5eea049c645eca6d27e59048f2f9", sha256)
            val legacy = pm.getPackageArchiveInfo(release.absolutePath, PackageManager.GET_SIGNATURES)
            assertNotNull("Legacy signature reader could not parse the release archive", legacy)
            assertArrayEquals(cert, requireNotNull(requireNotNull(legacy).signatures).single().toByteArray())
        } finally {
            release.delete()
        }
    }
}
