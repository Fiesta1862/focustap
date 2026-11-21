package com.focustap.app.data

/**
 * A user-defined focus profile. Packages are matched as prefixes so that
 * `com.google.android.youtube` also covers related package variants.
 */
data class FocusMode(
    val id: String,
    val name: String,
    val symbol: String,
    val blockedPackages: List<String> = emptyList(),
    val blockedDomains: List<String> = emptyList(),
)

data class FocusSession(
    val modeId: String,
    val modeName: String,
    val startedAt: Long,
    val endedAt: Long,
    val unlockReason: String,
)

data class AppState(
    val onboardingComplete: Boolean = false,
    val pairedTagId: String? = null,
    val pairedQrSecret: String? = null,
    val modes: List<FocusMode> = DEFAULT_MODES,
    val activeModeId: String = "deep_work",
    val isLocked: Boolean = false,
    val lockStartedAt: Long = 0L,
    val strictMode: Boolean = false,
    val emergencyUnlocksUsed: Int = 0,
    val emergencyWindowStart: Long = 0L,
    val emergencyCooldownUntil: Long = 0L,
    val sessions: List<FocusSession> = emptyList(),
    val lastUnlockReason: String = "",
) {
    val activeMode: FocusMode
        get() = modes.firstOrNull { it.id == activeModeId } ?: modes.firstOrNull() ?: FocusMode("custom", "Focus", "◎")

    val emergencyUnlocksRemaining: Int
        get() = (EMERGENCY_LIMIT - emergencyUnlocksUsed).coerceAtLeast(0)

    companion object {
        const val EMERGENCY_LIMIT = 3
        const val EMERGENCY_COOLDOWN_MS = 60_000L
    }
}

/** Apps that are never blocked: communication, navigation and the blocker itself. */
val ESSENTIAL_PACKAGES: List<String> = listOf(
    "com.focustap.app",
    "com.google.android.dialer",
    "com.android.dialer",
    "com.google.android.apps.messaging",
    "com.android.messaging",
    "com.google.android.apps.maps",
    "com.android.settings",
    "com.android.systemui",
    "com.google.android.gms",
)

val DEFAULT_MODES: List<FocusMode> = listOf(
    FocusMode(
        id = "deep_work",
        name = "Deep Work",
        symbol = "◆",
        blockedPackages = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.google.android.youtube",
            "com.tiktok.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
        ),
        blockedDomains = listOf("instagram.com", "facebook.com", "youtube.com", "tiktok.com", "reddit.com"),
    ),
    FocusMode(
        id = "study",
        name = "Study",
        symbol = "▲",
        blockedPackages = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.tiktok.android",
            "com.snapchat.android",
        ),
        blockedDomains = listOf("youtube.com", "tiktok.com", "instagram.com"),
    ),
    FocusMode(
        id = "sleep",
        name = "Sleep",
        symbol = "☾",
        blockedPackages = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.google.android.youtube",
            "com.tiktok.android",
            "com.snapchat.android",
            "com.whatsapp",
            "com.reddit.frontpage",
        ),
        blockedDomains = listOf("instagram.com", "facebook.com", "youtube.com", "tiktok.com", "reddit.com"),
    ),
)

fun Long.isSameMonthAs(other: Long): Boolean {
    val a = java.util.Calendar.getInstance().apply { timeInMillis = this@isSameMonthAs }
    val b = java.util.Calendar.getInstance().apply { timeInMillis = other }
    return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
        a.get(java.util.Calendar.MONTH) == b.get(java.util.Calendar.MONTH)
}
