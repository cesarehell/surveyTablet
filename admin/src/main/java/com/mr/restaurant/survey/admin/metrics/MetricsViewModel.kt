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
import kotlinx.coroutines.async
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

	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events: SharedFlow<AdminUiEvent> = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null, partialErrors = emptyList()) }

		supervisorScope {
			val summaryDef = async { safeCall { api.summary(tenantId) } }
			val dailyDef = async { safeCall { api.daily(tenantId) } }
			val tablesDef = async { safeCall { api.topTables(tenantId) } }
			val waitersDef = async { safeCall { api.topWaiters(tenantId) } }
			val rulesDef = async { safeCall { api.rules(tenantId) } }

			val summaryRes = summaryDef.await()
			val dailyRes = dailyDef.await()
			val tablesRes = tablesDef.await()
			val waitersRes = waitersDef.await()
			val rulesRes = rulesDef.await()

			val summary = (summaryRes as? ApiResult.Ok)?.value
			val errors = buildList {
				(summaryRes as? ApiResult.Err)?.message?.let(::add)
				(dailyRes as? ApiResult.Err)?.message?.let(::add)
				(tablesRes as? ApiResult.Err)?.message?.let(::add)
				(waitersRes as? ApiResult.Err)?.message?.let(::add)
				(rulesRes as? ApiResult.Err)?.message?.let(::add)
			}.distinct()

			_state.update { st ->
				st.copy(
					loading = false,
					summary = summary ?: st.summary,
					daily = (dailyRes as? ApiResult.Ok)?.value ?: st.daily,
					topTables = (tablesRes as? ApiResult.Ok)?.value ?: st.topTables,
					topWaiters = (waitersRes as? ApiResult.Ok)?.value ?: st.topWaiters,
					rules = (rulesRes as? ApiResult.Ok)?.value ?: st.rules,
					error = if (summary == null) errors.firstOrNull() else null,
					partialErrors = if (summary != null) errors else emptyList()
				)
			}

			if (summary != null && errors.isNotEmpty()) {
				_events.tryEmit(AdminUiEvent.ShowError(errors.first()))
			}
		}
	}
}
