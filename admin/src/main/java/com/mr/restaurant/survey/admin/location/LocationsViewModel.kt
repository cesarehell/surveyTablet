package com.mr.restaurant.survey.admin.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.core.location.api.LocationApi
import com.mr.restaurant.survey.core.location.dto.CreateLocationRequest
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
import com.mr.restaurant.survey.core.net.ApiFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationsUiState(
	val loading: Boolean = false,
	val locations: List<LocationDto> = emptyList(),
	val error: String? = null
)

class LocationsViewModel(
	private val baseUrl: String = "http://192.168.100.69:3040/"
) : ViewModel() {

	private val api = ApiFactory.retrofit(baseUrl).create(LocationApi::class.java)
	private val _state = MutableStateFlow(LocationsUiState())
	val state = _state.asStateFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		runCatching { api.list(tenantId) }
			.onSuccess { list -> _state.update { it.copy(loading = false, locations = list) } }
			.onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Error") } }
	}

	fun create(
		tenantId: String,
		name: String,
		city: String?,
		branchName: String?,
		code: String?,
		onDone: () -> Unit
	) = viewModelScope.launch {
		if (name.isBlank()) return@launch
		_state.update { it.copy(loading = true, error = null) }
		runCatching { api.create(CreateLocationRequest(tenantId, name, city, branchName, code)) }
			.onSuccess { onDone(); load(tenantId) }
			.onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Error") } }
	}

	suspend fun pairingCodes(locationId: String): List<PairingCodeDto> =
		api.pairingCodes(locationId)
}


