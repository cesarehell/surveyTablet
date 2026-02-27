package com.mr.restaurant.survey.admin.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.metrics.api.MetricsApi
import com.mr.restaurant.survey.core.metrics.dto.MetricsDayAggDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsItemCountDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsRuleAggDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsSummaryDto
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

data class MetricsUiState(
	val loading: Boolean = false,
	val summary: MetricsSummaryDto? = null,
	val daily: List<MetricsDayAggDto> = emptyList(),
	val topTables: List<MetricsItemCountDto> = emptyList(),
	val topWaiters: List<MetricsItemCountDto> = emptyList(),
	val rules: List<MetricsRuleAggDto> = emptyList(),
	val error: String? = null,
	val partialErrors: List<String> = emptyList()
)

@HiltViewModel
class MetricsViewModel @Inject constructor(
	private val api: MetricsApi
) : ViewModel() {
	private val _state = MutableStateFlow(MetricsUiState())
	val state = _state.asStateFlow()
	private var loadJob: Job? = null
	private var refreshJob: Job? = null

	@Volatile
	private var activeRequestId: Long = 0
	private var loadedTenantId: String? = null

	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String, forceFull: Boolean = false) {
		if (!forceFull && loadedTenantId == tenantId && _state.value.summary != null) return
		refreshJob?.cancel()
		loadJob?.cancel()
		val requestId = nextRequestId()
		loadJob = viewModelScope.launch {
			updateIfCurrent(requestId) { it.copy(loading = true, error = null, partialErrors = emptyList()) }
			val eventShown = AtomicBoolean(false)

			suspend fun registerError(message: String) {
				updateIfCurrent(requestId) { st ->
					if (st.summary == null) st.copy(error = message)
					else st.copy(partialErrors = (st.partialErrors + message).distinct())
				}
				if (eventShown.compareAndSet(false, true)) {
					_events.tryEmit(AdminUiEvent.ShowError(message))
				}
			}

			when (val summary = safeCall { api.summary(tenantId) }) {
				is ApiResult.Ok -> updateIfCurrent(requestId) { st ->
					loadedTenantId = tenantId
					st.copy(summary = summary.value, error = null)
				}

				is ApiResult.Err -> {
					registerError(summary.message)
					updateIfCurrent(requestId) { it.copy(loading = false) }
					return@launch
				}
			}

			val pendingCalls = AtomicInteger(4)
			suspend fun finishCall() {
				if (pendingCalls.decrementAndGet() == 0) {
					updateIfCurrent(requestId) { it.copy(loading = false) }
				}
			}

			supervisorScope {
				launch {
					try {
						when (val res = safeCall { api.daily(tenantId) }) {
							is ApiResult.Ok -> updateIfCurrent(requestId) { st -> st.copy(daily = res.value) }
							is ApiResult.Err -> registerError(res.message)
						}
					} finally {
						finishCall()
					}
				}

				launch {
					try {
						when (val res = safeCall { api.topTables(tenantId) }) {
							is ApiResult.Ok -> updateIfCurrent(requestId) { st -> st.copy(topTables = res.value) }
							is ApiResult.Err -> registerError(res.message)
						}
					} finally {
						finishCall()
					}
				}

				launch {
					try {
						when (val res = safeCall { api.topWaiters(tenantId) }) {
							is ApiResult.Ok -> updateIfCurrent(requestId) { st -> st.copy(topWaiters = res.value) }
							is ApiResult.Err -> registerError(res.message)
						}
					} finally {
						finishCall()
					}
				}

				launch {
					try {
						when (val res = safeCall { api.rules(tenantId) }) {
							is ApiResult.Ok -> updateIfCurrent(requestId) { st -> st.copy(rules = res.value) }
							is ApiResult.Err -> registerError(res.message)
						}
					} finally {
						finishCall()
					}
				}
			}
		}
	}

	fun refresh(tenantId: String) {
		refreshJob?.cancel()
		val requestId = nextRequestId()
		refreshJob = viewModelScope.launch {
			updateIfCurrent(requestId) { it.copy(loading = true, error = null) }
			when (val res = safeCall { api.summary(tenantId) }) {
				is ApiResult.Ok -> updateIfCurrent(requestId) { st ->
					st.copy(
						loading = false,
						summary = res.value,
						error = null
					)
				}

				is ApiResult.Err -> {
					updateIfCurrent(requestId) { it.copy(loading = false, error = res.message) }
					_events.tryEmit(AdminUiEvent.ShowError(res.message))
				}
			}
		}
	}

	private fun nextRequestId(): Long {
		activeRequestId += 1
		return activeRequestId
	}

	private inline fun updateIfCurrent(
		requestId: Long,
		transform: (MetricsUiState) -> MetricsUiState
	) {
		if (requestId == activeRequestId) {
			_state.update(transform)
		}
	}
}
