package com.mr.restaurant.survey.admin.tenant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TenantDashboardScreen(
	tenantId: String,
	onBack: () -> Unit,
	onLocations: () -> Unit,
	onTemplates: () -> Unit,
	onThresholds: () -> Unit,
	onAlerts: () -> Unit,
) {
	Column(Modifier
		.fillMaxSize()
		.padding(16.dp)) {
		Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
			TextButton(onClick = onBack) { Text("← Tenants") }
			Text("Tenant", style = MaterialTheme.typography.titleMedium)
		}

		Spacer(Modifier.height(8.dp))
		Text(tenantId, style = MaterialTheme.typography.titleLarge)

		Spacer(Modifier.height(16.dp))

		Button(onClick = onLocations, modifier = Modifier.fillMaxWidth()) { Text("Locations") }
		Spacer(Modifier.height(10.dp))
		Button(onClick = onTemplates, modifier = Modifier.fillMaxWidth()) { Text("Encuestas (Templates + Preguntas)") }
		Spacer(Modifier.height(10.dp))
		Button(onClick = onThresholds, modifier = Modifier.fillMaxWidth()) { Text("Reglas / Thresholds") }
		Spacer(Modifier.height(10.dp))
		Button(onClick = onAlerts, modifier = Modifier.fillMaxWidth()) { Text("Alertas") }

		Spacer(Modifier.height(16.dp))
		Text(
			"Nota: Editar/borrar Locations requiere endpoints (hoy tu backend sólo tiene create/list/get + pairing codes + active template).",
			style = MaterialTheme.typography.bodySmall
		)
	}
}
