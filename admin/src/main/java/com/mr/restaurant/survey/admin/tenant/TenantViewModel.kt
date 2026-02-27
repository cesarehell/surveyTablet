package com.mr.restaurant.survey.admin.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import com.mr.restaurant.survey.core.tenant.TenantRepository
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class TenantViewModel @Inject constructor(
	private val repo: TenantRepository
) : ViewModel() {
	private val _state = MutableStateFlow(TenantUiState())
	val state: StateFlow<TenantUiState> = _state.asStateFlow()
	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load() = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		when (val res = safeCall { repo.list() }) {
			is ApiResult.Ok -> _state.update { it.copy(loading = false, tenants = res.value) }
			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	fun create(name: String) = viewModelScope.launch {
		if (name.isBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("El nombre es obligatorio"))
			return@launch
		}
		_state.update { it.copy(loading = true, error = null) }

		when (val createRes = safeCall { repo.create(name) }) {
			is ApiResult.Ok -> {
				refreshAfterMutation()
				_events.tryEmit(AdminUiEvent.ShowSuccess("Tenant creado"))
			}

			is ApiResult.Err -> {
				val msg = if ((createRes.cause as? HttpException)?.code() == 409) {
					"Ya existe ese tenant."
				} else {
					createRes.message
				}
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(msg))
			}
		}
	}

	fun toggle(t: TenantDto) = viewModelScope.launch {
		val newStatus = if (t.status == TenantStatus.ACTIVE) TenantStatus.INACTIVE else TenantStatus.ACTIVE
		val previousTenants = _state.value.tenants
		_state.update { current ->
			current.copy(
				loading = false,
				error = null,
				tenants = current.tenants.map { tenant ->
					if (tenant.id == t.id) tenant.copy(status = newStatus) else tenant
				}
			)
		}

		when (val res = safeCall { repo.setStatus(t.id, newStatus) }) {
			is ApiResult.Ok -> {
				refreshAfterMutation()
				val message = if (newStatus == TenantStatus.ACTIVE) {
					"Tenant activado"
				} else {
					"Tenant desactivado"
				}
				_events.tryEmit(AdminUiEvent.ShowSuccess(message))
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false, tenants = previousTenants) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	private suspend fun refreshAfterMutation() {
		when (val refreshRes = safeCall { repo.list() }) {
			is ApiResult.Ok -> _state.update {
				it.copy(
					loading = false,
					error = null,
					tenants = refreshRes.value
				)
			}

			is ApiResult.Err -> _state.update { it.copy(loading = false, error = refreshRes.message) }
		}
	}
}

data class TenantUiState(
	val loading: Boolean = false,
	val tenants: List<TenantDto> = emptyList(),
	val error: String? = null
)
