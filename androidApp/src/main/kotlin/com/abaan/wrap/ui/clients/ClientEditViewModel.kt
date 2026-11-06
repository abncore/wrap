package com.abaan.wrap.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abaan.wrap.shared.data.ClientRepository
import com.abaan.wrap.shared.data.SettingsRepository
import com.abaan.wrap.shared.model.AppSettings
import com.abaan.wrap.shared.model.Client
import com.abaan.wrap.shared.model.NEW_ID
import com.abaan.wrap.shared.money.MoneyFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientForm(
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val paymentTerms: String = "",
    val defaultRate: String = "",
    val notes: String = "",
)

enum class ClientFormError { NAME_REQUIRED, TERMS_INVALID, RATE_INVALID }

data class ClientEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val form: ClientForm = ClientForm(),
    val errors: Set<ClientFormError> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val deleteBlocked: Boolean = false,
    /** Set once saving or deleting finished, so the screen can close. */
    val done: Boolean = false,
)

class ClientEditViewModel(
    private val clientId: Long,
    private val clients: ClientRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ClientEditUiState(isNew = clientId == NEW_ID))
    val state: StateFlow<ClientEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val appSettings = settings.getSettings()
            val existing = if (clientId != NEW_ID) clients.getClient(clientId) else null
            _state.update {
                it.copy(
                    loading = false,
                    settings = appSettings,
                    form = existing?.toForm() ?: it.form,
                )
            }
        }
    }

    fun update(transform: (ClientForm) -> ClientForm) {
        _state.update { it.copy(form = transform(it.form), errors = emptySet()) }
    }

    fun save() {
        val form = _state.value.form
        val errors = buildSet {
            if (form.name.isBlank()) add(ClientFormError.NAME_REQUIRED)
            if (form.paymentTerms.isNotBlank() && form.paymentTerms.toIntOrNull() == null) add(ClientFormError.TERMS_INVALID)
            if (form.defaultRate.isNotBlank() && MoneyFormat.parse(form.defaultRate) == null) add(ClientFormError.RATE_INVALID)
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            clients.save(
                Client(
                    id = clientId,
                    name = form.name,
                    company = form.company,
                    phone = form.phone,
                    email = form.email,
                    paymentTermsDays = form.paymentTerms.takeIf { it.isNotBlank() }?.toInt(),
                    defaultRate = form.defaultRate.takeIf { it.isNotBlank() }?.let(MoneyFormat::parse),
                    notes = form.notes,
                ),
            )
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        if (clientId == NEW_ID) return
        viewModelScope.launch {
            if (clients.delete(clientId)) {
                _state.update { it.copy(done = true) }
            } else {
                _state.update { it.copy(deleteBlocked = true) }
            }
        }
    }

    fun dismissDeleteBlocked() {
        _state.update { it.copy(deleteBlocked = false) }
    }

    private fun Client.toForm() = ClientForm(
        name = name,
        company = company,
        phone = phone,
        email = email,
        paymentTerms = paymentTermsDays?.toString().orEmpty(),
        defaultRate = defaultRate?.let(MoneyFormat::toInput).orEmpty(),
        notes = notes,
    )
}
