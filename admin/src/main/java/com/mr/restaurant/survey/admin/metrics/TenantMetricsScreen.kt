package com.mr.restaurant.survey.admin.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mr.restaurant.survey.admin.R
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.admin.ui.adminThresholdTypeLabel
import com.mr.restaurant.survey.core.common.DisplayDateTimeFormatter
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
	val lifecycleOwner = LocalLifecycleOwner.current
	val skipFirstResumeRefresh = remember(tenantId) { mutableStateOf(true) }

	LaunchedEffect(tenantId) { vm.load(tenantId, forceFull = false) }
	DisposableEffect(lifecycleOwner, tenantId) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				if (skipFirstResumeRefresh.value) {
					skipFirstResumeRefresh.value = false
				} else {
					vm.refresh(tenantId)
				}
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}
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
			SimpleTopBar(title = stringResource(R.string.metrics_title), subtitle = tenantId, onBack = onBack)

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
						stringResource(R.string.metrics_window_last_30_days),
						style = MaterialTheme.typography.titleLarge
					)
					Button(
						onClick = { vm.refresh(tenantId) },
						enabled = !st.loading
					) { Text(stringResource(R.string.common_refresh)) }
				}

				if (st.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

				st.error?.let { message ->
					ErrorWithRetry(
						message = message,
						onRetry = { vm.load(tenantId, forceFull = true) },
						modifier = Modifier.fillMaxWidth()
					)
				}

				if (st.error == null && st.summary == null && !st.loading) {
					EmptyState(stringResource(R.string.metrics_load_error), modifier = Modifier.weight(1f))
				} else if (st.summary != null) {
					st.partialErrors.firstOrNull()?.let { ErrorBanner(it) }
					LazyColumn(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						item { SummarySection(st) }
						item { DailySection(st) }
						item { TopSection(title = stringResource(R.string.metrics_top_tables), items = st.topTables) }
						item { TopSection(title = stringResource(R.string.metrics_top_waiters), items = st.topWaiters) }
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
		MetricsSectionTitle(stringResource(R.string.metrics_summary_title))
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			MetricCard(
				stringResource(R.string.metrics_instances),
				s.instances.toString(),
				Modifier.weight(1f),
				accent = MaterialTheme.colorScheme.primary
			)
			MetricCard(
				stringResource(R.string.metrics_submitted),
				s.submitted.toString(),
				Modifier.weight(1f),
				accent = MaterialTheme.colorScheme.secondary
			)
		}
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			MetricCard(
				stringResource(R.string.metrics_alerts),
				s.alertsInWindow.toString(),
				Modifier.weight(1f),
				accent = MaterialTheme.colorScheme.tertiary
			)
			MetricCard(
				stringResource(R.string.metrics_conversion),
				"${(s.conversionRate * 100).toInt()}%",
				Modifier.weight(1f),
				accent = MaterialTheme.colorScheme.primary
			)
		}
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			MetricCard(
				stringResource(R.string.metrics_open),
				s.openAlertsNow.toString(),
				Modifier.weight(1f),
				accent = MaterialTheme.colorScheme.error
			)
			MetricCard(
				stringResource(R.string.metrics_ack),
				s.ackAlertsNow.toString(),
				Modifier.weight(1f),
				accent = Color(0xFF2E7D32)
			)
		}
		AlertsOpsSummary(
			open = s.openAlertsNow,
			ack = s.ackAlertsNow,
			alertsInWindow = s.alertsInWindow,
			submitted = s.submitted
		)
		Text(
			stringResource(
				R.string.metrics_window_range,
				DisplayDateTimeFormatter.humanDate(s.from),
				DisplayDateTimeFormatter.humanDate(s.to)
			),
			style = MaterialTheme.typography.bodySmall
		)
	}
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, accent: Color) {
	Card(
		modifier,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
	) {
		Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Box(
				Modifier
					.size(width = 28.dp, height = 4.dp)
					.background(accent, shape = MaterialTheme.shapes.small)
			)
			Text(label, style = MaterialTheme.typography.bodySmall)
			Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
		}
	}
}

@Composable
private fun DailySection(st: MetricsUiState) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		MetricsSectionTitle(stringResource(R.string.metrics_daily_title))
		if (st.daily.isEmpty()) {
			Text(stringResource(R.string.metrics_daily_empty), style = MaterialTheme.typography.bodySmall)
			return@Column
		}
		MetricsCardSection {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				Text(
					stringResource(R.string.metrics_daily_last10),
					style = MaterialTheme.typography.bodySmall,
					fontWeight = FontWeight.SemiBold
				)
				BarChartRows(
					items = st.daily.takeLast(10).reversed().map {
						BarChartItem(
							label = DisplayDateTimeFormatter.humanDayShort(it.day),
							value = it.submitted
						)
					},
					barColor = MaterialTheme.colorScheme.primary
				)
				HorizontalDivider()
				st.daily.takeLast(10).reversed().forEach { day ->
					Text(
						"${DisplayDateTimeFormatter.humanDate(day.day)}: inst=${day.instances}, enviadas=${day.submitted}, alertas=${day.alerts}",
						style = MaterialTheme.typography.bodySmall
					)
				}
			}
		}
	}
}

@Composable
private fun AlertsOpsSummary(open: Long, ack: Long, alertsInWindow: Long, submitted: Long) {
	val total = open + ack
	val ackRate = if (total <= 0) 0f else ack.toFloat() / total.toFloat()
	val attendedPct = (ackRate * 100).toInt()
	val alertsPer100 = if (submitted <= 0) 0f else (alertsInWindow.toFloat() / submitted.toFloat()) * 100f
	val pendingColor = MaterialTheme.colorScheme.error
	val attendedColor = Color(0xFF2E7D32)
	MetricsCardSection {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text("Atención de alertas", fontWeight = FontWeight.SemiBold)
			if (total <= 0) {
				Text(
					"Sin alertas en la ventana actual",
					style = MaterialTheme.typography.bodySmall
				)
			} else {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.Center
				) {
					AlertAttentionDonut(
						ackRate = ackRate,
						pendingColor = pendingColor,
						attendedColor = attendedColor,
						modifier = Modifier.size(184.dp)
					)
				}
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
					verticalAlignment = Alignment.CenterVertically
				) {
					AlertLegendStat(
						label = "Pendientes",
						value = open,
						color = pendingColor
					)
					AlertLegendStat(
						label = "Atendidas",
						value = ack,
						color = attendedColor
					)
				}
			}
			Text(
				"Alertas por 100 encuestas: ${"%.1f".format(alertsPer100)}",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun AlertAttentionDonut(
	ackRate: Float,
	pendingColor: Color,
	attendedColor: Color,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val stroke = size.minDimension * 0.12f
			val diameter = size.minDimension - stroke
			val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
			val arcSize = Size(diameter, diameter)
			drawArc(
				color = pendingColor.copy(alpha = 0.45f),
				startAngle = -90f,
				sweepAngle = 360f,
				useCenter = false,
				topLeft = topLeft,
				size = arcSize,
				style = Stroke(width = stroke, cap = StrokeCap.Round)
			)
			drawArc(
				color = attendedColor,
				startAngle = -90f,
				sweepAngle = (ackRate.coerceIn(0f, 1f) * 360f),
				useCenter = false,
				topLeft = topLeft,
				size = arcSize,
				style = Stroke(width = stroke, cap = StrokeCap.Round)
			)
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				text = "${(ackRate * 100).toInt()}%",
				style = MaterialTheme.typography.headlineMedium,
				fontWeight = FontWeight.Bold
			)
			Text(
				text = "Atendidas",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun AlertLegendStat(
	label: String,
	value: Long,
	color: Color,
) {
	Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
		Box(
			modifier = Modifier
				.size(10.dp)
				.clip(RoundedCornerShape(999.dp))
				.background(color)
		)
		Text(value.toString(), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
		Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
}

@Composable
private fun TopSection(title: String, items: List<MetricsItemCountDto>) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		MetricsSectionTitle(title)
		if (items.isEmpty()) {
			Text(stringResource(R.string.metrics_no_data), style = MaterialTheme.typography.bodySmall)
			return@Column
		}
		MetricsCardSection {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
				BarChartRows(
					items = items.map { item ->
						BarChartItem(label = item.key, value = item.count)
					},
					barColor = MaterialTheme.colorScheme.secondary
				)
				HorizontalDivider()
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
		MetricsSectionTitle(stringResource(R.string.metrics_rules_title))
		if (rules.isEmpty()) {
			Text(stringResource(R.string.metrics_rules_empty), style = MaterialTheme.typography.bodySmall)
			return@Column
		}
		val fallbackRuleName = stringResource(R.string.metrics_rule_name_fallback)
		val fallbackValue = stringResource(R.string.metrics_value_fallback)
		MetricsCardSection {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				val topRules = rules.take(10)
				BarChartRows(
					items = topRules.mapIndexed { index, r ->
						BarChartItem(
							label = "${index + 1}. ${adminThresholdTypeLabel(r.type ?: fallbackRuleName)}",
							value = r.alerts
						)
					},
					barColor = MaterialTheme.colorScheme.tertiary
				)
				HorizontalDivider()
				topRules.forEachIndexed { index, r ->
					Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
						Text(
							"${index + 1}. ${adminThresholdTypeLabel(r.type ?: fallbackValue)} · ${r.alerts} alerta(s)",
							style = MaterialTheme.typography.bodySmall,
							fontWeight = FontWeight.SemiBold
						)
						r.questionText?.trim()?.takeIf { it.isNotBlank() }?.let { questionText ->
							Text(
								"Pregunta: $questionText",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.primary
							)
						}
						val summaryParts = buildList {
							add(if (r.active == false) "Inactiva" else "Activa")
							add("Umbral: ${r.threshold ?: fallbackValue}")
							add("Cooldown: ${r.cooldownMin ?: 0} min")
							r.questionId?.trim()?.takeIf { it.isNotBlank() }?.let { add("Q: $it") }
							r.lastSeen?.let { add("Última: ${DisplayDateTimeFormatter.humanDateTime(it)}") }
						}
						Text(
							summaryParts.joinToString(" · "),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
					if (index < rules.take(10).lastIndex) {
						HorizontalDivider()
					}
				}
			}
		}
	}
}

@Composable
private fun MetricsSectionTitle(title: String) {
	Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MetricsCardSection(content: @Composable () -> Unit) {
	Card(
		Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
	) {
		content()
	}
}

private data class BarChartItem(
	val label: String,
	val value: Long,
	val suffix: String = ""
)

@Composable
private fun BarChartRows(
	items: List<BarChartItem>,
	barColor: Color,
	rowColors: Map<String, Color> = emptyMap()
) {
	if (items.isEmpty()) return
	val maxValue = (items.maxOfOrNull { it.value } ?: 0L).coerceAtLeast(1L).toFloat()
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		items.forEach { item ->
			val rowColor = rowColors[item.label] ?: barColor
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Text(
						item.label,
						modifier = Modifier.weight(1f),
						style = MaterialTheme.typography.bodySmall,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
					Text(
						"${item.value}${item.suffix}",
						style = MaterialTheme.typography.bodySmall,
						fontWeight = FontWeight.SemiBold
					)
				}
				LinearProgressIndicator(
					progress = { item.value / maxValue },
					modifier = Modifier
						.fillMaxWidth()
						.height(10.dp),
					color = rowColor,
					trackColor = MaterialTheme.colorScheme.surfaceVariant
				)
			}
		}
	}
}
