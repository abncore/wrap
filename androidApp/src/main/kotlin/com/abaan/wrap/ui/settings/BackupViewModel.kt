package com.abaan.wrap.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.files.BackupManager
import com.abaan.wrap.files.PendingRestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BackupMessage { SAVED, SAVE_FAILED, NOT_A_BACKUP, RESTORED, RESTORE_FAILED }

data class BackupUiState(
    val lastBackupAt: Long? = null,
    val working: Boolean = false,
    /** A backup that was read and is waiting for the user to confirm replacing everything. */
    val pending: PendingRestore? = null,
    val message: BackupMessage? = null,
)

class BackupViewModel(
    private val manager: BackupManager,
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState(lastBackupAt = manager.lastBackupAt))
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun save(uri: Uri) = run(success = BackupMessage.SAVED, failure = BackupMessage.SAVE_FAILED) {
        manager.write(uri)
        _state.update { it.copy(lastBackupAt = manager.lastBackupAt) }
    }

    fun open(uri: Uri) = run(success = null, failure = BackupMessage.NOT_A_BACKUP) {
        val pending = manager.read(uri)
        _state.update { it.copy(pending = pending) }
    }

    fun confirmRestore() {
        val pending = _state.value.pending ?: return
        _state.update { it.copy(pending = null) }
        run(success = BackupMessage.RESTORED, failure = BackupMessage.RESTORE_FAILED) { manager.restore(pending) }
    }

    fun cancelRestore() = _state.update { it.copy(pending = null) }

    fun messageShown() = _state.update { it.copy(message = null) }

    private fun run(success: BackupMessage?, failure: BackupMessage, block: suspend () -> Unit) {
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            val message = try {
                block()
                success
            } catch (e: Exception) {
                Log.w("Backup", "Backup step failed", e)
                failure
            }
            _state.update { it.copy(working = false, message = message) }
        }
    }
}
