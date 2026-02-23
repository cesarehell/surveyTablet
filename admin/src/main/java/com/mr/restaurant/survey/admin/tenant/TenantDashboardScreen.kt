package com.mr.restaurant.survey.admin.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TenantDashboardScreen(
	tenantId: String,
	onBack: () -> Unit,
	onMetrics: () -> Unit,
	onLocations: () -> Unit,
	onTemplates: () -> Unit,
	onThresholds: () -> Unit,
	onAlerts: () -> Unit,
) {
	val actions = listOf(
		DashAction("Locations", "Sucursales y códigos", onLocations),
		DashAction("Encuestas", "Templates y preguntas", onTemplates),
		DashAction("Métricas", "Resumen y tendencias", onMetrics),
		DashAction("Thresholds", "Reglas operativas", onThresholds),
		DashAction("Alertas", "Alertas abiertas", onAlerts)
	)

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		DashboardHero(tenantId = tenantId, onBack = onBack)

		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 16.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp)
		) {
			item {
				Spacer(Modifier.height(8.dp))
				Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					InsightCard(
						title = "Acciones",
						value = actions.size.toString(),
						subtitle = "Módulos disponibles",
						modifier = Modifier.weight(1f)
					)
					InsightCard(
						title = "Estado",
						value = "Live",
						subtitle = "Admin conectado",
						modifier = Modifier.weight(1f)
					)
				}
			}

			item {
				Text(
					"Operación",
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(top = 8.dp)
				)
			}

				item {
					LazyVerticalGrid(
					columns = GridCells.Fixed(2),
					modifier = Modifier.height(320.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp),
					userScrollEnabled = false
					) {
						gridItems(actions.take(4)) { action ->
							ActionCard(action)
						}
					}
				}

			items(actions.drop(4)) { action ->
				ActionWideCard(action)
			}

			item {
				Card(
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer
					),
					modifier = Modifier.fillMaxWidth()
				) {
					Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
						Text(
							"Nota técnica",
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.secondary
						)
						Text(
							"Editar/borrar Locations aún depende de endpoints backend dedicados. Hoy el flujo soportado es create/list/get + pairing codes + active template.",
							style = MaterialTheme.typography.bodySmall
						)
					}
				}
				Spacer(Modifier.height(16.dp))
			}
		}
	}
}

@Composable
private fun DashboardHero(
	tenantId: String,
	onBack: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.primary)
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			TextButton(onClick = onBack) {
				Text("← Tenants", color = MaterialTheme.colorScheme.onPrimary)
			}
			Surface(
				color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
				shape = MaterialTheme.shapes.medium
			) {
				Text(
					"Admin",
					modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
					color = MaterialTheme.colorScheme.onPrimary,
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.Bold
				)
			}
		}

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
			Box(
				modifier = Modifier
					.size(56.dp)
					.clip(MaterialTheme.shapes.large)
					.background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.13f)),
				contentAlignment = Alignment.Center
			) {
				Text("MR", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
			}
			Column {
				Text(
					tenantId,
					color = MaterialTheme.colorScheme.onPrimary,
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold
				)
				Text(
					"Panel operativo del tenant",
					color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
					style = MaterialTheme.typography.bodySmall
				)
			}
		}
	}
}

@Composable
private fun InsightCard(
	title: String,
	value: String,
	subtitle: String,
	modifier: Modifier = Modifier
) {
	Card(
		modifier = modifier,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
	) {
		Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
			Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
			Text(subtitle, style = MaterialTheme.typography.bodySmall)
		}
	}
}

@Composable
private fun ActionCard(action: DashAction) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = action.onClick),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(12.dp),
			verticalArrangement = Arrangement.SpaceBetween
		) {
			Box(
				modifier = Modifier
					.size(40.dp)
					.clip(MaterialTheme.shapes.medium)
					.background(MaterialTheme.colorScheme.primaryContainer),
				contentAlignment = Alignment.Center
			) {
				Text(action.title.take(1), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
			}
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(action.title, fontWeight = FontWeight.Bold)
				Text(action.subtitle, style = MaterialTheme.typography.bodySmall)
			}
		}
	}
}

@Composable
private fun ActionWideCard(action: DashAction) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = action.onClick),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Column(Modifier.weight(1f)) {
				Text(action.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
				Text(action.subtitle, style = MaterialTheme.typography.bodySmall)
			}
			Text("Abrir", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
		}
	}
}

private data class DashAction(
	val title: String,
	val subtitle: String,
	val onClick: () -> Unit
)
