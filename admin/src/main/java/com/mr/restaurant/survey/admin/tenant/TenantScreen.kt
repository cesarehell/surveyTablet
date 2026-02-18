package com.mr.restaurant.survey.admin.tenant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus

@Composable
fun TenantScreen(
	state: TenantUiState,
	onCreateTenant: (String) -> Unit,
	onToggleTenant: (TenantDto) -> Unit,
	onOpenTenant: (String) -> Unit
) {
	var showCreate by remember { mutableStateOf(false) }
	var name by remember { mutableStateOf("") }

	Column(
		Modifier
			.fillMaxSize()
			.padding(16.dp)
	) {
		Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
			Text("Tenants", style = MaterialTheme.typography.titleLarge)
			Button(onClick = { showCreate = true }) { Text("Crear") }
		}

		state.error?.let {
			Spacer(Modifier.height(8.dp))
			Text(it, color = MaterialTheme.colorScheme.error)
		}

		Spacer(Modifier.height(12.dp))
		if (state.loading) CircularProgressIndicator()
		Spacer(Modifier.height(12.dp))

		LazyColumn {
			items(state.tenants, key = { it.id }) { t ->
				TenantRow(
					t = t,
					onOpen = { onOpenTenant(t.id) },
					onToggle = { onToggleTenant(t) }
				)
				HorizontalDivider()
			}
		}
	}

	if (showCreate) {
		AlertDialog(
			onDismissRequest = { showCreate = false },
			title = { Text("Crear tenant") },
			text = {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text("Nombre") },
					singleLine = true
				)
			},
			confirmButton = {
				Button(onClick = {
					val trimmed = name.trim()
					if (trimmed.isNotEmpty()) onCreateTenant(trimmed)
					name = ""
					showCreate = false
				}) { Text("Crear") }
			},
			dismissButton = {
				TextButton(onClick = { showCreate = false }) { Text("Cancelar") }
			}
		)
	}
}

@Composable
private fun TenantRow(
	t: TenantDto,
	onOpen: () -> Unit,
	onToggle: () -> Unit
) {
	Row(
		Modifier
			.fillMaxWidth()
			.clickable(onClick = onOpen)
			.padding(vertical = 10.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column {
			Text(t.name, style = MaterialTheme.typography.titleMedium)
			Text(t.id, style = MaterialTheme.typography.bodySmall)
		}
		Row(verticalAlignment = Alignment.CenterVertically) {
			Text(if (t.status == TenantStatus.ACTIVE) "ACTIVE" else "INACTIVE")
			Spacer(Modifier.width(8.dp))
			Switch(
				checked = t.status == TenantStatus.ACTIVE,
				onCheckedChange = { onToggle() }
			)
		}
	}
}
