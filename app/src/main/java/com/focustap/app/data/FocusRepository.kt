package com.focustap.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focustap.app.core.CrashLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "focustap_prefs")

enum class EmergencyResult { OK, COOLDOWN, EXHAUSTED }

private object Keys {
    val ONBOARDING = booleanPreferencesKey("onboarding_complete")
    val PAIRED_TAG = stringPreferencesKey("paired_tag_id")
    val PAIRED_QR = stringPreferencesKey("paired_qr_secret")
    val MODES = stringPreferencesKey("modes")
    val ACTIVE_MODE = stringPreferencesKey("active_mode_id")
    val IS_LOCKED = booleanPreferencesKey("is_locked")
    val LOCK_STARTED = longPreferencesKey("lock_started_at")
    val STRICT = booleanPreferencesKey("strict_mode")
    val EMERGENCY_USED = intPreferencesKey("emergency_used")
    val EMERGENCY_WINDOW = longPreferencesKey("emergency_window_start")
    val EMERGENCY_COOLDOWN = longPreferencesKey("emergency_cooldown_until")
    val SESSIONS = stringPreferencesKey("sessions")
    val LAST_UNLOCK = stringPreferencesKey("last_unlock_reason")
}

class FocusRepository(private val context: Context) {

    private val gson = Gson()

    val state: Flow<AppState> = context.focusDataStore.data
        .catch { exception ->
            CrashLogger.record(exception, "data-store-read")
            emit(emptyPreferences())
        }
        .map { prefs ->
            val now = System.currentTimeMillis()
            val window = prefs[Keys.EMERGENCY_WINDOW] ?: 0L
            val used = if (window > 0L && window.isSameMonthAs(now)) {
                prefs[Keys.EMERGENCY_USED] ?: 0
            } else {
                0
            }
            val cooldown = prefs[Keys.EMERGENCY_COOLDOWN] ?: 0L
            AppState(
                onboardingComplete = prefs[Keys.ONBOARDING] == true,
                pairedTagId = prefs[Keys.PAIRED_TAG],
                pairedQrSecret = prefs[Keys.PAIRED_QR],
                modes = parse(prefs[Keys.MODES], DEFAULT_MODES).ifEmpty { DEFAULT_MODES },
                activeModeId = prefs[Keys.ACTIVE_MODE] ?: "deep_work",
                isLocked = prefs[Keys.IS_LOCKED] == true,
                lockStartedAt = prefs[Keys.LOCK_STARTED] ?: 0L,
                strictMode = prefs[Keys.STRICT] == true,
                emergencyUnlocksUsed = used,
                emergencyWindowStart = window,
                emergencyCooldownUntil = if (cooldown > now) cooldown else 0L,
                sessions = parse(prefs[Keys.SESSIONS], emptyList()),
                lastUnlockReason = prefs[Keys.LAST_UNLOCK] ?: "",
            )
        }

    private inline fun <reified T> parse(raw: String?, fallback: T): T =
        if (raw.isNullOrBlank()) {
            fallback
        } else {
            try {
                gson.fromJson<T>(raw, object : TypeToken<T>() {}.type) ?: fallback
            } catch (_: Exception) {
                fallback
            }
        }

    suspend fun setOnboardingComplete() = edit { it[Keys.ONBOARDING] = true }

    suspend fun setPairedTag(id: String?) = edit {
        if (id == null) it.remove(Keys.PAIRED_TAG) else it[Keys.PAIRED_TAG] = id
    }

    suspend fun setPairedQr(secret: String?) = edit {
        if (secret == null) it.remove(Keys.PAIRED_QR) else it[Keys.PAIRED_QR] = secret
    }

    suspend fun setActiveMode(id: String) = edit { it[Keys.ACTIVE_MODE] = id }

    suspend fun setStrict(enabled: Boolean) = edit { it[Keys.STRICT] = enabled }

    suspend fun saveMode(mode: FocusMode) = edit { prefs ->
        val modes = parse<List<FocusMode>>(prefs[Keys.MODES], DEFAULT_MODES).toMutableList()
        val index = modes.indexOfFirst { it.id == mode.id }
        if (index >= 0) modes[index] = mode else modes.add(mode)
        prefs[Keys.MODES] = gson.toJson(modes)
    }

    suspend fun deleteMode(id: String) = edit { prefs ->
        val modes = parse<List<FocusMode>>(prefs[Keys.MODES], DEFAULT_MODES)
            .filterNot { it.id == id }
        if (modes.isEmpty()) {
            prefs[Keys.MODES] = gson.toJson(DEFAULT_MODES)
            prefs[Keys.ACTIVE_MODE] = DEFAULT_MODES.first().id
        } else {
            prefs[Keys.MODES] = gson.toJson(modes)
            if (prefs[Keys.ACTIVE_MODE] == id) prefs[Keys.ACTIVE_MODE] = modes.first().id
        }
    }

    suspend fun startSession(modeId: String) = edit { prefs ->
        prefs[Keys.ACTIVE_MODE] = modeId
        prefs[Keys.IS_LOCKED] = true
        prefs[Keys.LOCK_STARTED] = System.currentTimeMillis()
    }

    suspend fun endSession(reason: String) = edit { prefs ->
        recordSession(prefs, reason)
    }

    suspend fun emergencyUnlock(): EmergencyResult {
        var result = EmergencyResult.EXHAUSTED
        edit { prefs ->
            val now = System.currentTimeMillis()
            val window = prefs[Keys.EMERGENCY_WINDOW] ?: 0L
            val used = if (window > 0L && window.isSameMonthAs(now)) {
                prefs[Keys.EMERGENCY_USED] ?: 0
            } else {
                0
            }
            val cooldown = prefs[Keys.EMERGENCY_COOLDOWN] ?: 0L

            result = when {
                used >= AppState.EMERGENCY_LIMIT -> EmergencyResult.EXHAUSTED
                now < cooldown -> EmergencyResult.COOLDOWN
                else -> EmergencyResult.OK
            }
            if (result != EmergencyResult.OK) return@edit

            prefs[Keys.EMERGENCY_WINDOW] = now
            prefs[Keys.EMERGENCY_USED] = used + 1
            prefs[Keys.EMERGENCY_COOLDOWN] = now + AppState.EMERGENCY_COOLDOWN_MS
            recordSession(prefs, "emergency")
        }
        return result
    }

    suspend fun resetEmergencyAllowance() = edit { prefs ->
        prefs.remove(Keys.EMERGENCY_WINDOW)
        prefs.remove(Keys.EMERGENCY_USED)
        prefs.remove(Keys.EMERGENCY_COOLDOWN)
    }

    suspend fun clearAll() {
        context.focusDataStore.edit { it.clear() }
    }

    private fun recordSession(prefs: MutablePreferences, reason: String) {
        val started = prefs[Keys.LOCK_STARTED] ?: 0L
        val wasLocked = prefs[Keys.IS_LOCKED] == true
        if (wasLocked && started > 0L) {
            val modeId = prefs[Keys.ACTIVE_MODE] ?: DEFAULT_MODES.first().id
            val modeName = parse<List<FocusMode>>(prefs[Keys.MODES], DEFAULT_MODES)
                .firstOrNull { it.id == modeId }?.name ?: "Focus"
            val existing = parse<List<FocusSession>>(prefs[Keys.SESSIONS], emptyList())
            val ended = System.currentTimeMillis().coerceAtLeast(started)
            prefs[Keys.SESSIONS] = gson.toJson(
                listOf(FocusSession(modeId, modeName, started, ended, reason)) + existing.take(99),
            )
        }
        prefs[Keys.IS_LOCKED] = false
        prefs[Keys.LOCK_STARTED] = 0L
        prefs[Keys.LAST_UNLOCK] = reason
    }

    private suspend fun edit(transform: (MutablePreferences) -> Unit) {
        context.focusDataStore.edit(transform)
    }
}
