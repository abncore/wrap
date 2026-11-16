package com.abaan.wrap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.ProfileRepository
import com.abaan.wrap.shared.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: Profile = Profile(),
    /** Next invoice number as typed. */
    val nextNumber: String = "1",
    val nextNumberInvalid: Boolean = false,
    val done: Boolean = false,
)

class ProfileViewModel(
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profiles.getProfile()
            _state.update { it.copy(loading = false, profile = profile, nextNumber = profile.nextInvoiceNumber.toString()) }
        }
    }

    fun update(transform: (Profile) -> Profile) = _state.update { it.copy(profile = transform(it.profile)) }

    fun setNextNumber(value: String) = _state.update { it.copy(nextNumber = value, nextNumberInvalid = false) }

    fun save() {
        val state = _state.value
        val next = state.nextNumber.toLongOrNull()?.takeIf { it >= 1 }
        if (next == null) {
            _state.update { it.copy(nextNumberInvalid = true) }
            return
        }
        viewModelScope.launch {
            profiles.save(state.profile.copy(nextInvoiceNumber = next))
            _state.update { it.copy(done = true) }
        }
    }
}
