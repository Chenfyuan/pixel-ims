package io.github.vvb2060.ims.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionUtilsTest {

    @Test
    fun parseVersionHandlesStandardFormat() {
        val v = VersionUtils.parseVersion("3.9.1.r120.abcdef12")
        assertNotNull(v)
        assertEquals(listOf(3, 9, 1), v.baseParts)
        assertEquals(120, v.revisionCode)
        assertEquals(2, v.channelRank)
    }

    @Test
    fun parseVersionHandlesDebugChannel() {
        val v = VersionUtils.parseVersion("3.9.1.d55.abcdef12")
        assertNotNull(v)
        assertEquals(55, v.revisionCode)
        assertEquals(1, v.channelRank)
    }

    @Test
    fun parseVersionHandlesVPrefix() {
        val v = VersionUtils.parseVersion("v3.9.0")
        assertNotNull(v)
        assertEquals(listOf(3, 9, 0), v.baseParts)
    }

    @Test
    fun parseVersionReturnsNullForGarbage() {
        assertNull(VersionUtils.parseVersion("notaversion"))
        assertNull(VersionUtils.parseVersion(""))
    }

    @Test
    fun parseVersionHandlesTwoParts() {
        val v = VersionUtils.parseVersion("3.9")
        assertNotNull(v)
        assertEquals(listOf(3, 9), v.baseParts)
    }

    @Test
    fun isVersionNewerDetectsNewerBase() {
        assertTrue(VersionUtils.isVersionNewer("3.10.0", "3.9.1"))
        assertTrue(VersionUtils.isVersionNewer("4.0.0", "3.9.1"))
        assertFalse(VersionUtils.isVersionNewer("3.9.0", "3.9.1"))
    }

    @Test
    fun isVersionNewerDetectsNewerRevision() {
        assertTrue(VersionUtils.isVersionNewer("3.9.1.r120.abc", "3.9.1.r100.def"))
        assertFalse(VersionUtils.isVersionNewer("3.9.1.r100.abc", "3.9.1.r120.def"))
    }

    @Test
    fun isVersionNewerReleaseBeatsDebug() {
        assertTrue(VersionUtils.isVersionNewer("3.9.1.r100.abc", "3.9.1.d100.def"))
        assertFalse(VersionUtils.isVersionNewer("3.9.1.d100.abc", "3.9.1.r100.def"))
    }

    @Test
    fun isVersionNewerSameVersionIsNotNewer() {
        assertFalse(VersionUtils.isVersionNewer("3.9.1", "3.9.1"))
    }

    @Test
    fun isVersionNewerFallsBackToStringCompareWhenUnparseable() {
        assertTrue(VersionUtils.isVersionNewer("abc", "def"))
        assertFalse(VersionUtils.isVersionNewer("same", "same"))
    }

    @Test
    fun normalizeCountryIsoTrimsAndLowercases() {
        assertEquals("cn", VersionUtils.normalizeCountryIso("  CN "))
        assertEquals("us", VersionUtils.normalizeCountryIso("US"))
        assertEquals("", VersionUtils.normalizeCountryIso(""))
    }

    @Test
    fun sanitizeCountryIsoInputFiltersAndTruncates() {
        assertEquals("cn", VersionUtils.sanitizeCountryIsoInput("CN"))
        assertEquals("12345678", VersionUtils.sanitizeCountryIsoInput("123456789"))
        assertEquals("abc", VersionUtils.sanitizeCountryIsoInput("  A!B@C# "))
    }

    @Test
    fun sanitizeMccInputHandlesSimpleMcc() {
        assertEquals("460", VersionUtils.sanitizeMccInput("460"))
        assertEquals("310", VersionUtils.sanitizeMccInput("  310 "))
        assertEquals("", VersionUtils.sanitizeMccInput(""))
    }

    @Test
    fun sanitizeMccInputHandlesRange() {
        assertEquals("310-316", VersionUtils.sanitizeMccInput("310-316"))
        assertEquals("440-441", VersionUtils.sanitizeMccInput("440-441"))
    }

    @Test
    fun sanitizeMccInputStripsInvalidChars() {
        assertEquals("460", VersionUtils.sanitizeMccInput("460abc"))
        assertEquals("310-316", VersionUtils.sanitizeMccInput("310-316xyz"))
    }

    @Test
    fun sanitizeMccInputTruncatesLongInput() {
        assertEquals("1234567", VersionUtils.sanitizeMccInput("12345678"))
    }
}
