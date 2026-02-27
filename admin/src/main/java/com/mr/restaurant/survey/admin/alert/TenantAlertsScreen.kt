package com.mr.restaurant.survey.admin.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.R
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.core.alert.dto.AlertViewDto
import com.mr.restaurant.survey.core.common.DisplayDateTimeFormatter

@Composable
fun TenantAlertsScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: AlertViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.NavigateToTemplateDetail -> Unit
				AdminUiEvent.CloseDialog -> Unit
			}
		}
	}

	Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			SimpleTopBar(
				title = stringResource(R.string.alerts_screen_title),
				subtitle = tenantId,
				onBack = onBack
			)

			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						stringResource(R.string.alerts_open_title),
						style = MaterialTheme.typography.titleLarge
					)
					Button(onClick = { vm.load(tenantId) }, enabled = !st.loading) { Text(stringResource(R.string.common_refresh)) }
				}

				if (st.loading) {
					LinearProgressIndicator(Modifier.fillMaxWidth())
				}

				if (st.error != null && st.alerts.isEmpty()) {
					ErrorWithRetry(
						message = st.error ?: "Error",
						onRetry = { vm.load(tenantId) }
					)
				} else {
					st.error?.let { ErrorBanner(it) }
					if (!st.loading && st.alerts.isEmpty()) {
						EmptyState(
							message = stringResource(R.string.alerts_empty),
							modifier = Modifier.weight(1f)
						)
					} else {
						LazyColumn(
							modifier = Modifier.weight(1f),
							verticalArrangement = Arrangement.spacedBy(8.dp)
						) {
							items(st.alerts, key = { it.id }) { alert ->
								AlertCard(
									alert = alert,
									busy = alert.id in st.acknowledgingIds,
									onAck = { vm.ack(alert.id) }
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun AlertCard(
	alert: AlertViewDto,
	busy: Boolean,
	onAck: () -> Unit
) {
	Card(Modifier.fillMaxWidth()) {
		Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(
				alert.reason ?: stringResource(R.string.alerts_reason_fallback),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold
			)
			Text(
				stringResource(
					R.string.alerts_severity_state,
					alert.severity ?: "-",
					alert.state ?: "-"
				),
				style = MaterialTheme.typography.bodySmall
			)
			val meta = listOfNotNull(
				alert.tableNo?.let { stringResource(R.string.alerts_table_meta, it) },
				alert.waiterName?.let { stringResource(R.string.alerts_waiter_meta, it) },
				alert.ruleType?.let { stringResource(R.string.alerts_rule_meta, it) }
			).joinToString(" · ")
			if (meta.isNotBlank()) {
				Text(meta, style = MaterialTheme.typography.bodySmall)
			}
			alert.startedAt?.let {
				Text(
					stringResource(R.string.alerts_started_at, DisplayDateTimeFormatter.humanDateTime(it)),
					style = MaterialTheme.typography.bodySmall
				)
			}
			if (alert.escalated) {
				EscalationText(alert = alert)
			}
			HorizontalDivider()
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				TextButton(onClick = onAck, enabled = !busy) {
					Text(if (busy) stringResource(R.string.alerts_ack_pending) else stringResource(R.string.alerts_ack))
				}
			}
		}
	}
}

@Composable
private fun EscalationText(alert: AlertViewDto) {
	val base = stringResource(R.string.alerts_escalated)
	val suffix = alert.escalatedAt?.let { " · ${DisplayDateTimeFormatter.humanDateTime(it)}" }.orEmpty()
	Text("$base$suffix", color = MaterialTheme.colorScheme.error)
}
