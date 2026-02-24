package com.mr.restaurant.survey.admin.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.location.api.LocationApi
import com.mr.restaurant.survey.core.location.dto.CreateLocationRequest
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
import com.mr.restaurant.survey.core.location.dto.UpdateLocationRequest
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationsUiState(
	val loading: Boolean = false,
	val locations: List<LocationDto> = emptyList(),
	val error: String? = null,
	val pairingCodesLoading: Boolean = false,
	val pairingCodes: List<PairingCodeDto> = emptyList(),
	val pairingCodesError: String? = null
)

@HiltViewModel
class LocationsViewModel @Inject constructor(
	private val api: LocationApi
) : ViewModel() {
	private val _state = MutableStateFlow(LocationsUiState())
	val state = _state.asStateFlow()
	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update {
			it.copy(
				loading = true,
				error = null,
				pairingCodesLoading = false,
				pairingCodes = emptyList(),
				pairingCodesError = null
			)
		}

		when (val res = safeCall { api.list(tenantId) }) {
			is ApiResult.Ok -> _state.update { it.copy(loading = false, locations = res.value) }
			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	fun create(
		tenantId: String,
		name: String,
		city: String?,
		branchName: String?,
		code: String?
	) = viewModelScope.launch {
		if (name.isBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("El nombre es obligatorio"))
			return@launch
		}
		_state.update { it.copy(loading = true) }

		when (val res = safeCall { api.create(CreateLocationRequest(tenantId, name, city, branchName, code)) }) {
			is ApiResult.Ok -> {
				_state.update {
					it.copy(
						loading = false,
						locations = it.locations + res.value
					)
				}
				_events.tryEmit(AdminUiEvent.ShowSuccess("Location creada"))
				_events.tryEmit(AdminUiEvent.CloseDialog)
			}

			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun loadPairingCodes(locationId: String) = viewModelScope.launch {
		_state.update {
			it.copy(
				pairingCodesLoading = true,
				pairingCodesError = null,
				pairingCodes = emptyList()
			)
		}

		when (val res = safeCall { api.pairingCodes(locationId) }) {
			is ApiResult.Ok -> _state.update {
				it.copy(
					pairingCodesLoading = false,
					pairingCodes = res.value
				)
			}

			is ApiResult.Err -> _state.update {
				it.copy(
					pairingCodesLoading = false,
					pairingCodesError = res.message
				)
			}
		}
	}

	fun updateLocation(
		locationId: String,
		name: String,
		city: String?,
		branchName: String?,
		code: String?,
		active: Boolean
	) = viewModelScope.launch {
		if (name.isBlank()) {
			_events.tryEmit(AdminUiEvent.ShowError("El nombre es obligatorio"))
			return@launch
		}
		_state.update { it.copy(loading = true) }
		when (val res = safeCall {
			api.update(
				id = locationId,
				req = UpdateLocationRequest(
					name = name,
					city = city,
					branchName = branchName,
					code = code,
					active = active
				)
			)
		}) {
			is ApiResult.Ok -> {
				_state.update { state ->
					state.copy(
						loading = false,
						locations = state.locations.map { if (it.id == locationId) res.value else it }
					)
				}
				_events.tryEmit(AdminUiEvent.ShowSuccess("Location actualizada"))
			}
			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun softDeleteLocation(locationId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true) }
		when (val res = safeCall { api.delete(locationId) }) {
			is ApiResult.Ok -> {
				_state.update { state ->
					state.copy(
						loading = false,
						locations = state.locations.map { loc ->
							if (loc.id == locationId) loc.copy(active = false) else loc
						}
					)
				}
				_events.tryEmit(AdminUiEvent.ShowSuccess("Location desactivada"))
			}
			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}

	fun clearPairingCodes() {
		_state.update {
			it.copy(
				pairingCodesLoading = false,
				pairingCodesError = null,
				pairingCodes = emptyList()
			)
		}
	}

}
