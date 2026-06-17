package com.lumiai.flashlight.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.lumiai.flashlight.core.domain.model.ProStatus
import com.lumiai.flashlight.core.domain.model.isProActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages temporary Pro access earned via rewarded ads.
 *
 * Cost escalation (resets at midnight each day):
 *   Unlock #1 today → 3 ads
 *   Unlock #2 today → 6 ads
 *   Unlock #3 today → 12 ads
 *   ...each unlock doubles the cost until midnight reset.
 *
 * State is persisted in DataStore so it survives process death.
 *
 * Keys:
 *   REWARDED_EXPIRES_AT      — epoch ms when current Pro window ends (0 = none)
 *   REWARDED_ADS_TODAY       — total ads watched today
 *   REWARDED_UNLOCKS_TODAY   — number of unlocks granted today (drives cost doubling)
 *   REWARDED_DAY_EPOCH       — calendar day key (yyyyMMdd) to detect midnight rollover
 *   REWARDED_ADS_PENDING     — ads watched toward the NEXT unlock (resets on each grant)
 */
@Singleton
class RewardedProRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val EXPIRES_AT      = longPreferencesKey("rewarded_expires_at")
        private val ADS_TODAY       = intPreferencesKey("rewarded_ads_today")
        private val UNLOCKS_TODAY   = intPreferencesKey("rewarded_unlocks_today")
        private val DAY_EPOCH       = intPreferencesKey("rewarded_day_epoch")
        private val ADS_PENDING     = intPreferencesKey("rewarded_ads_pending")

        const val PRO_DURATION_MS   = 3_600_000L   // 1 hour
        const val BASE_AD_COST      = 3            // first unlock costs 3 ads
    }

    /** Current rewarded state as a Flow — combine with BillingRepository in ViewModel. */
    val rewardedStatusFlow: Flow<RewardedState> = dataStore.data.map { prefs ->
        val today      = todayKey()
        val savedDay   = prefs[DAY_EPOCH] ?: 0
        val rolledOver = savedDay != today

        // Midnight rollover — treat counters as reset
        val expiresAt     = if (rolledOver) 0L else prefs[EXPIRES_AT]     ?: 0L
        val adsToday      = if (rolledOver) 0  else prefs[ADS_TODAY]      ?: 0
        val unlocksToday  = if (rolledOver) 0  else prefs[UNLOCKS_TODAY]  ?: 0
        val adsPending    = if (rolledOver) 0  else prefs[ADS_PENDING]    ?: 0

        val nextCost = BASE_AD_COST * (1 shl unlocksToday)   // 3 * 2^unlocks

        RewardedState(
            expiresAt     = expiresAt,
            adsWatchedToday = adsToday,
            unlocksToday  = unlocksToday,
            adsPending    = adsPending,
            nextCost      = nextCost,
        )
    }

    /**
     * Call after each rewarded ad is successfully dismissed (fully watched).
     * Returns the updated state — if adsPending reaches nextCost, Pro is granted.
     */
    suspend fun onAdWatched(): RewardedState {
        var result = RewardedState()
        dataStore.edit { prefs ->
            val today     = todayKey()
            val savedDay  = prefs[DAY_EPOCH] ?: 0
            if (savedDay != today) {
                // Midnight rollover — reset counters
                prefs[DAY_EPOCH]      = today
                prefs[ADS_TODAY]      = 0
                prefs[UNLOCKS_TODAY]  = 0
                prefs[ADS_PENDING]    = 0
                prefs[EXPIRES_AT]     = 0L
            }

            val adsToday     = (prefs[ADS_TODAY]     ?: 0) + 1
            val unlocksToday = prefs[UNLOCKS_TODAY]  ?: 0
            val adsPending   = (prefs[ADS_PENDING]   ?: 0) + 1
            val nextCost     = BASE_AD_COST * (1 shl unlocksToday)

            prefs[ADS_TODAY]   = adsToday
            prefs[ADS_PENDING] = adsPending

            val newExpiresAt: Long
            val newUnlocks: Int
            val newPending: Int

            if (adsPending >= nextCost) {
                // Grant Pro for 1 hour
                newExpiresAt = System.currentTimeMillis() + PRO_DURATION_MS
                newUnlocks   = unlocksToday + 1
                newPending   = 0
                prefs[EXPIRES_AT]    = newExpiresAt
                prefs[UNLOCKS_TODAY] = newUnlocks
                prefs[ADS_PENDING]   = 0
            } else {
                newExpiresAt = prefs[EXPIRES_AT] ?: 0L
                newUnlocks   = unlocksToday
                newPending   = adsPending
            }

            val newNextCost = BASE_AD_COST * (1 shl newUnlocks)
            result = RewardedState(
                expiresAt       = newExpiresAt,
                adsWatchedToday = adsToday,
                unlocksToday    = newUnlocks,
                adsPending      = newPending,
                nextCost        = newNextCost,
            )
        }
        return result
    }

    /** Revoke active rewarded Pro (e.g. on permanent purchase — not needed but clean). */
    suspend fun revoke() {
        dataStore.edit { it[EXPIRES_AT] = 0L }
    }

    private fun todayKey(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 10000 +
               (c.get(Calendar.MONTH) + 1) * 100 +
               c.get(Calendar.DAY_OF_MONTH)
    }
}

data class RewardedState(
    val expiresAt: Long       = 0L,
    val adsWatchedToday: Int  = 0,
    val unlocksToday: Int     = 0,
    val adsPending: Int       = 0,
    val nextCost: Int         = RewardedProRepository.BASE_AD_COST,
) {
    val isActive: Boolean get() = expiresAt > System.currentTimeMillis()
    val adsNeeded: Int    get() = (nextCost - adsPending).coerceAtLeast(0)
    /** Minutes remaining in the current Pro window (0 if not active). */
    val minutesRemaining: Int get() = if (!isActive) 0
        else ((expiresAt - System.currentTimeMillis()) / 60_000L).toInt().coerceAtLeast(0)
}
