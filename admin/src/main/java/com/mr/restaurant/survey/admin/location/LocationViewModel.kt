package com.mr.restaurant.survey.admin.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.core.location.api.LocationApi
import com.mr.restaurant.survey.core.location.dto.CreateLocationRequest
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
import com.mr.restaurant.survey.core.net.ApiFactory
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationsUiState(
	val loading: Boolean = false,
	val locations: List<LocationDto> = emptyList(),
	val error: String? = null,
	val pairingCodesLoading: Boolean = false,
	val pairingCodes: List<PairingCodeDto> = emptyList(),
	val pairingCodesError: String? = null
)

class LocationsViewModel() : ViewModel() {
	private val api = ApiFactory
		.retrofit()
		.create(LocationApi::class.java)
	private val _state = MutableStateFlow(LocationsUiState())
	val state = _state.asStateFlow()

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
		code: String?,
		onDone: () -> Unit
	) = viewModelScope.launch {

		if (name.isBlank()) return@launch
		_state.update { it.copy(loading = true, error = null) }

		when (val res = safeCall { api.create(CreateLocationRequest(tenantId, name, city, branchName, code)) }) {
			is ApiResult.Ok -> {
				_state.update {
					it.copy(
						loading = false,
						locations = it.locations + res.value
					)
				}
				onDone()
			}

			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
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
