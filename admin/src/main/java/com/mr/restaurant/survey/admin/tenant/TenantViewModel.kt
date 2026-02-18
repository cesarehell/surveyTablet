package com.mr.restaurant.survey.admin.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.core.net.ApiFactory
import com.mr.restaurant.survey.core.tenant.TenantRepository
import com.mr.restaurant.survey.core.tenant.api.TenantApi
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class TenantViewModel : ViewModel() {

	private val api = ApiFactory
		.retrofit("http://192.168.100.69:3040/")   // ojo: sin salto de línea
		.create(TenantApi::class.java)
	private val repo = TenantRepository(api)
	private val _state = MutableStateFlow(TenantUiState())
	val state: StateFlow<TenantUiState> = _state.asStateFlow()

	fun load() = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		runCatching { repo.list() }
			.onSuccess { list -> _state.update { it.copy(loading = false, tenants = list) } }
			.onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Error") } }
	}

	fun create(name: String) = viewModelScope.launch {
		if (name.isBlank()) return@launch
		_state.update { it.copy(loading = true, error = null) }
		runCatching { repo.create(name) }
			.onSuccess { load() }
			.onFailure { e ->
				val msg = if ((e as? HttpException)?.code() == 409) "Ya existe ese tenant." else (e.message ?: "Error")
				_state.update { it.copy(loading = false, error = msg) }
			}
	}

	fun toggle(t: TenantDto) = viewModelScope.launch {
		val newStatus = if (t.status == TenantStatus.ACTIVE) TenantStatus.INACTIVE else TenantStatus.ACTIVE
		runCatching { repo.setStatus(t.id, newStatus) }
			.onSuccess { load() }
			.onFailure { e -> _state.update { it.copy(error = e.message ?: "Error") } }
	}
}

data class TenantUiState(
	val loading: Boolean = false,
	val tenants: List<TenantDto> = emptyList(),
	val error: String? = null
)