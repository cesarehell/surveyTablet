package com.mr.restaurant.survey.admin.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.alert.api.AlertApi
import com.mr.restaurant.survey.core.alert.dto.AlertViewDto
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

data class AlertsUiState(
	val loading: Boolean = false,
	val alerts: List<AlertViewDto> = emptyList(),
	val error: String? = null,
	val acknowledgingIds: Set<String> = emptySet()
)

@HiltViewModel
class AlertViewModel @Inject constructor(
	private val alertApi: AlertApi
) : ViewModel() {
	private val _state = MutableStateFlow(AlertsUiState())
	val state = _state.asStateFlow()

	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		when (val res = safeCall { alertApi.list(tenantId) }) {
			is ApiResult.Ok -> _state.update { it.copy(loading = false, alerts = res.value, error = null) }
			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	fun ack(alertId: String) = viewModelScope.launch {
		setAckBusy(alertId, true)
		when (val res = safeCall { alertApi.ack(alertId) }) {
			is ApiResult.Ok -> {
				_state.update { st -> st.copy(alerts = st.alerts.filterNot { it.id == alertId }) }
				_events.tryEmit(AdminUiEvent.ShowSuccess("Alerta confirmada"))
			}
			is ApiResult.Err -> _events.tryEmit(AdminUiEvent.ShowError(res.message))
		}
		setAckBusy(alertId, false)
	}

	private fun setAckBusy(alertId: String, busy: Boolean) {
		_state.update { st ->
			val next = st.acknowledgingIds.toMutableSet()
			if (busy) next += alertId else next -= alertId
			st.copy(acknowledgingIds = next)
		}
	}
}
