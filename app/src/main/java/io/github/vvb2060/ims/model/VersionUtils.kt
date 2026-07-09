package io.github.vvb2060.ims.model

import java.util.Locale

object VersionUtils {

    data class ParsedVersion(
        val baseParts: List<Int>,
        val revisionCode: Int,
        val channelRank: Int,
    )

    fun parseVersion(version: String): ParsedVersion? {
        val normalized = version.trim().removePrefix("v").removePrefix("V")
        val baseMatch = Regex("\\d+(?:\\.\\d+){1,2}").find(normalized) ?: return null
        val baseParts = baseMatch.value.split('.').map { it.toIntOrNull() ?: 0 }
        val suffix = normalized.substring(baseMatch.range.last + 1)
        val channelMatch = Regex("(?:^|[._-])([rRdD])(\\d+)").find(suffix)
        val channel = channelMatch?.groupValues?.getOrNull(1)?.lowercase(Locale.US)
        val revisionCode = channelMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
        val channelRank = when (channel) {
            "r" -> 2
            "d" -> 1
            else -> 0
        }
        return ParsedVersion(baseParts, revisionCode, channelRank)
    }

    fun compareVersionParts(left: List<Int>, right: List<Int>): Int {
        val maxSize = maxOf(left.size, right.size)
        for (index in 0 until maxSize) {
            val l = left.getOrElse(index) { 0 }
            val r = right.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    fun isVersionNewer(latest: String, current: String): Boolean {
        val latestVersion = parseVersion(latest)
        val currentVersion = parseVersion(current)
        if (latestVersion == null || currentVersion == null) {
            return latest.trim() != current.trim()
        }
        val baseCompare = compareVersionParts(latestVersion.baseParts, currentVersion.baseParts)
        if (baseCompare != 0) {
            return baseCompare > 0
        }
        if (latestVersion.revisionCode != currentVersion.revisionCode) {
            return latestVersion.revisionCode > currentVersion.revisionCode
        }
        return latestVersion.channelRank > currentVersion.channelRank
    }

    fun normalizeCountryIso(value: String): String {
        return value.trim().lowercase(Locale.US)
    }

    fun sanitizeCountryIsoInput(value: String): String {
        return normalizeCountryIso(value)
            .filter { it.isLetterOrDigit() }
            .take(8)
    }

    fun sanitizeMccInput(value: String): String {
        val cleaned = value.trim().filter { it.isDigit() || it == '-' }
        if (cleaned.isEmpty()) return ""
        val firstDash = cleaned.indexOf('-')
        return if (firstDash == -1) {
            cleaned.take(7)
        } else {
            val left = cleaned.substring(0, firstDash).filter { it.isDigit() }.take(3)
            val right = cleaned.substring(firstDash + 1).filter { it.isDigit() }.take(3)
            if (right.isNotEmpty()) "$left-$right" else left
        }
    }
}
