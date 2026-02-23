package com.mr.restaurant.survey.admin.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.core.metrics.dto.MetricsItemCountDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsRuleAggDto

@Composable
fun TenantMetricsScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: MetricsViewModel = hiltViewModel()
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
			SimpleTopBar(title = "Métricas", subtitle = tenantId, onBack = onBack)

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
					Text("Últimos 30 días", style = MaterialTheme.typography.titleLarge)
					Button(onClick = { vm.load(tenantId) }, enabled = !st.loading) { Text("Actualizar") }
				}

				if (st.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

				st.error?.let { message ->
					ErrorWithRetry(message = message, onRetry = { vm.load(tenantId) }, modifier = Modifier.fillMaxWidth())
				}

				if (st.error == null && st.summary == null && !st.loading) {
					EmptyState("No se pudo cargar métricas.", modifier = Modifier.weight(1f))
				} else if (st.summary != null) {
					st.partialErrors.firstOrNull()?.let { ErrorBanner(it) }
					LazyColumn(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						item { SummarySection(st) }
						item { DailySection(st) }
						item { TopSection(title = "Top Mesas", items = st.topTables) }
						item { TopSection(title = "Top Meseros", items = st.topWaiters) }
						item { RulesSection(st.rules) }
					}
				}
			}
		}
	}
}

@Composable
private fun SummarySection(st: MetricsUiState) {
	val s = st.summary ?: return
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			MetricCard("Encuestas", s.instances.toString(), Modifier.weight(1f))
			MetricCard("Enviadas", s.submitted.toString(), Modifier.weight(1f))
		}
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			MetricCard("Alertas", s.alertsInWindow.toString(), Modifier.weight(1f))
			MetricCard("Conversión", "${(s.conversionRate * 100).toInt()}%", Modifier.weight(1f))
		}
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			MetricCard("Abiertas", s.openAlertsNow.toString(), Modifier.weight(1f))
			MetricCard("ACK", s.ackAlertsNow.toString(), Modifier.weight(1f))
		}
		Text("Ventana: ${s.from} → ${s.to}", style = MaterialTheme.typography.bodySmall)
	}
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
	Card(modifier) {
		Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Text(label, style = MaterialTheme.typography.bodySmall)
			Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
		}
	}
}

@Composable
private fun DailySection(st: MetricsUiState) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text("Serie Diaria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		if (st.daily.isEmpty()) {
			Text("Sin datos diarios", style = MaterialTheme.typography.bodySmall)
			return@Column
		}
		Card(Modifier.fillMaxWidth()) {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				st.daily.takeLast(10).reversed().forEach { day ->
					Text(
						"${day.day}: inst=${day.instances}, enviadas=${day.submitted}, alertas=${day.alerts}",
						style = MaterialTheme.typography.bodySmall
					)
				}
			}
		}
	}
}

@Composable
private fun TopSection(title: String, items: List<MetricsItemCountDto>) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		if (items.isEmpty()) {
			Text("Sin datos", style = MaterialTheme.typography.bodySmall)
			return@Column
		}
		Card(Modifier.fillMaxWidth()) {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				items.forEachIndexed { index, item ->
					Text("${index + 1}. ${item.key} · ${item.count}", style = MaterialTheme.typography.bodySmall)
				}
			}
		}
	}
}

@Composable
private fun RulesSection(rules: List<MetricsRuleAggDto>) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text("Efectividad de Reglas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
		if (rules.isEmpty()) {
			Text("Sin alertas asociadas a reglas en la ventana", style = MaterialTheme.typography.bodySmall)
			return@Column
		}
		Card(Modifier.fillMaxWidth()) {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				rules.take(10).forEachIndexed { index, r ->
					Text(
						"${index + 1}. ${r.type ?: "-"} (${r.ruleId ?: "sin id"}) · alertas=${r.alerts} · threshold=${r.threshold ?: "-"}",
						style = MaterialTheme.typography.bodySmall
					)
					Text(
						"activa=${r.active ?: false} · cooldown=${r.cooldownMin ?: 0} · lastSeen=${r.lastSeen ?: "-"}",
						style = MaterialTheme.typography.bodySmall
					)
					if (index < rules.take(10).lastIndex) {
						HorizontalDivider()
					}
				}
			}
		}
	}
}
