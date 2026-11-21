package com.focustap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focustap.app.core.FocusController
import com.focustap.app.data.AppState
import com.focustap.app.data.FocusMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FocusViewModel : ViewModel() {

    val state: StateFlow<AppState> = FocusController.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusController.cached,
    )

    val loaded: StateFlow<Boolean> = FocusController.stateLoaded

    val events = FocusController.events

    fun startFocus(modeId: String? = null) = FocusController.startFocus(modeId)
    fun endFocus() = FocusController.endSession("manual")
    fun emergencyUnlock() = FocusController.emergencyUnlock()
    fun setActiveMode(id: String) = FocusController.setActiveMode(id)
    fun setStrict(enabled: Boolean) = FocusController.setStrict(enabled)
    fun saveMode(mode: FocusMode) = FocusController.saveMode(mode)
    fun deleteMode(id: String) = FocusController.deleteMode(id)
    fun pairTag(id: String) = FocusController.pairTag(id)
    fun pairQr(secret: String) = FocusController.pairQr(secret)
    fun setAwaitingTagPairing(value: Boolean) { FocusController.awaitingTagPairing = value }
    fun setAwaitingQrPairing(value: Boolean) { FocusController.awaitingQrPairing = value }
    fun clearPairing() = FocusController.clearPairing()
    fun finishOnboarding() = FocusController.finishOnboarding()
    fun resetEmergencyAllowance() = FocusController.resetEmergencyAllowance()
    fun resetEverything() = FocusController.resetEverything()
}
