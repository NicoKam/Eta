package io.github.mangi.eta.data.update

import com.sun.net.httpserver.HttpServer
import io.github.mangi.eta.BuildConfig
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun newerPatchVersionIsNewer() {
        assertTrue(AppUpdateChecker.isNewer("3.0.7", "3.0.6"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(AppUpdateChecker.isNewer("3.0.5", "3.0.6"))
    }

    @Test
    fun missingSegmentsCountAsZero() {
        assertTrue(AppUpdateChecker.isNewer("3.1", "3.0.9"))
        assertFalse(AppUpdateChecker.isNewer("3.0", "3.0.1"))
    }

    @Test
    fun vPrefixIsIgnored() {
        assertTrue(AppUpdateChecker.isNewer("v3.1.0", "3.0.6"))
        assertFalse(AppUpdateChecker.isNewer("v3.0.6", "3.0.6"))
    }

    @Test
    fun debugSuffixIsIgnored() {
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6-debug"))
        assertTrue(AppUpdateChecker.isNewer("3.0.10", "3.0.9-debug"))
    }

    @Test
    fun buildMetadataIsIgnored() {
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6+a1b2c3d"))
        assertFalse(AppUpdateChecker.isNewer("3.0.6", "3.0.6-debug+a1b2c3d"))
        assertTrue(AppUpdateChecker.isNewer("3.0.7", "3.0.6+a1b2c3d"))
    }

    @Test
    fun updateEndpointUsesBuildTimeRepository() {
        assertEquals(
            "https://api.github.com/repos/${BuildConfig.UPDATE_REPOSITORY}/releases/latest",
            AppUpdateChecker.latestReleaseUrl(),
        )
        assertEquals(
            "https://api.github.com/repos/NicoKam/Eta/releases/latest",
            AppUpdateChecker.latestReleaseUrl("NicoKam/Eta"),
        )
        assertEquals(
            "https://api.github.com/repos/Mangi-11/Eta/releases/latest",
            AppUpdateChecker.latestReleaseUrl("Mangi-11/Eta"),
        )
    }

    @Test
    fun releaseRequestReturnsForkVersionAndDownloadPage() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val requestedPath = AtomicReference<String>()
        server.createContext("/repos/NicoKam/Eta/releases/latest") { exchange ->
            requestedPath.set(exchange.requestURI.path)
            val body = """{"tag_name":"v4.0.0-fork.20261126.1","html_url":"https://github.com/NicoKam/Eta/releases/tag/v4.0.0-fork.20261126.1"}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val url = AppUpdateChecker.latestReleaseUrl("NicoKam/Eta")
                .replace("https://api.github.com", "http://127.0.0.1:${server.address.port}")
            val release = AppUpdateChecker.fetchLatest(url)
            assertEquals("/repos/NicoKam/Eta/releases/latest", requestedPath.get())
            assertEquals("4.0.0-fork.20261126.1", release.version)
            assertEquals("https://github.com/NicoKam/Eta/releases/tag/v4.0.0-fork.20261126.1", release.url)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun unchangedForkDoesNotReportAnUpdate() {
        assertFalse(AppUpdateChecker.isNewer("4.0.0-fork.20261126", "4.0.0-fork.20261126"))
        assertFalse(AppUpdateChecker.isNewer("4.0.0-fork.20261126.1", "4.0.0-fork.20261126.1"))
        assertFalse(AppUpdateChecker.isNewer("4.0.0-fork.20261126.0", "4.0.0-fork.20261126"))
    }

    @Test
    fun forkDatesAndRevisionsCompareNumericallyInBothDirections() {
        val versions = listOf(
            "4.0.0-fork.20261125",
            "4.0.0-fork.20261126",
            "4.0.0-fork.20261126.1",
            "4.0.0-fork.20261126.9",
            "4.0.0-fork.20261126.10",
            "4.0.0-fork.20261127",
            "4.0.1-fork.20261125",
            "4.1.0-fork.20261125",
        )
        for ((latestIndex, latest) in versions.withIndex()) {
            for ((currentIndex, current) in versions.withIndex()) {
                assertEquals("$latest vs $current", latestIndex > currentIndex, AppUpdateChecker.isNewer(latest, current))
            }
        }
    }

    @Test
    fun forkDebugAndMetadataSuffixesDoNotAffectOrdering() {
        assertFalse(AppUpdateChecker.isNewer("v4.0.0-fork.20261126.1", "4.0.0-fork.20261126.1-debug+abc123"))
        assertFalse(AppUpdateChecker.isNewer("4.0.0-fork.20261126.1+abc123", "4.0.0-fork.20261126.1"))
        assertTrue(AppUpdateChecker.isNewer("v4.0.0-fork.20261126.2", "4.0.0-fork.20261126.1-debug+abc123"))
    }

    @Test
    fun forkComparisonKeepsMissingCoreSegmentsEquivalentToZero() {
        assertFalse(AppUpdateChecker.isNewer("4.0-fork.20261126", "4.0.0-fork.20261126"))
        assertTrue(AppUpdateChecker.isNewer("4.0-fork.20261127", "4.0.0-fork.20261126"))
    }

    @Test
    fun sameOfficialVersionDoesNotOverrideInstalledFork() {
        assertFalse(AppUpdateChecker.isNewer("4.0.0", "4.0.0-fork.20261126"))
        assertTrue(AppUpdateChecker.isNewer("4.0.0-fork.20261126", "4.0.0"))
        assertTrue(AppUpdateChecker.isNewer("4.0.1", "4.0.0-fork.20261126.49"))
    }
}
