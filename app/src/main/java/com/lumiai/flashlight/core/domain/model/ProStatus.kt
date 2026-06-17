package com.lumiai.flashlight.core.domain.model

/**
 * Represents the user's Pro access state.
 *
 * [Free]          — no purchase, no active reward
 * [Pro]           — permanent IAP purchase (pro_unlock)
 * [ProRewarded]   — temporary Pro earned by watching rewarded ads
 *                   expires after 1 hour; [adsWatchedToday] tracks cost escalation;
 *                   resets daily at midnight.
 * [Loading]       — billing client still connecting
 * [Error]         — billing error (treated as Free in UI)
 */
sealed class ProStatus {
    object Free    : ProStatus()
    object Pro     : ProStatus()
    data class ProRewarded(
        val expiresAt: Long,         // System.currentTimeMillis() + 3_600_000
        val adsWatchedToday: Int,    // total rewarded ads watched today (resets midnight)
        val nextCost: Int,           // ads needed for NEXT unlock = 3 * 2^(unlocks today)
    ) : ProStatus()
    object Loading : ProStatus()
    data class Error(val msg: String) : ProStatus()
}

/** True if the user currently has any form of Pro access (permanent OR active reward). */
val ProStatus.isProActive: Boolean
    get() = when (this) {
        is ProStatus.Pro         -> true
        is ProStatus.ProRewarded -> System.currentTimeMillis() < expiresAt
        else                     -> false
    }
