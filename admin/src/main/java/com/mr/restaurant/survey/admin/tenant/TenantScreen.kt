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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus

@Composable
fun TenantScreen(
	state: TenantUiState,
	snackbarHostState: SnackbarHostState,
	onRetry: () -> Unit,
	onCreateTenant: (String) -> Unit,
	onToggleTenant: (TenantDto) -> Unit,
	onOpenTenant: (String) -> Unit
) {
	var showCreate by remember { mutableStateOf(false) }
	var name by remember { mutableStateOf("") }

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		containerColor = MaterialTheme.colorScheme.background
	) { padding ->
		Column(
			Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			TenantListHero(
				total = state.tenants.count { it.status == TenantStatus.ACTIVE },
				loading = state.loading,
				onCreate = { showCreate = true }
			)

			Column(
				Modifier
					.fillMaxSize()
					.padding(horizontal = 16.dp)
			) {
				state.error?.let {
					Spacer(Modifier.height(8.dp))
					ErrorWithRetry(
						message = it,
						onRetry = onRetry
					)
				}

				Spacer(Modifier.height(12.dp))
				if (state.loading && state.tenants.isEmpty()) {
					Box(
						modifier = Modifier.fillMaxWidth(),
						contentAlignment = Alignment.Center
					) {
						CircularProgressIndicator()
					}
				}
				Spacer(Modifier.height(8.dp))

				if (!state.loading && state.tenants.isEmpty()) {
					EmptyState(
						message = "No hay tenants creados todavía.",
						modifier = Modifier.weight(1f)
					)
				} else {
					LazyColumn(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						items(state.tenants, key = { it.id }) { t ->
							TenantCard(
								t = t,
								enabled = !state.loading,
								onOpen = { onOpenTenant(t.id) },
								onToggle = { onToggleTenant(t) }
							)
						}
						item { Spacer(Modifier.height(16.dp)) }
					}
				}
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
					label = { Text("Nombre del restaurante") },
					singleLine = true
				)
			},
			confirmButton = {
				Button(
					onClick = {
						val trimmed = name.trim()
						if (trimmed.isNotEmpty()) onCreateTenant(trimmed)
						name = ""
						showCreate = false
					},
					enabled = name.isNotBlank() && !state.loading
				) { Text("Crear") }
			},
			dismissButton = {
				TextButton(onClick = { showCreate = false }) { Text("Cancelar") }
			}
		)
	}
}

@Composable
private fun TenantListHero(
	total: Int,
	loading: Boolean,
	onCreate: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.primary)
			.padding(16.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				Box(
					modifier = Modifier
						.size(42.dp)
						.clip(MaterialTheme.shapes.medium)
						.background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
					contentAlignment = Alignment.Center
				) {
					Text("MR", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
				}
				Column {
					Text(
						"Mr. Restaurant",
						color = MaterialTheme.colorScheme.onPrimary,
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold
					)
					Text(
						"Tenants / Franquicias",
						color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
						style = MaterialTheme.typography.bodySmall
					)
				}
			}
			Button(
				onClick = onCreate,
				enabled = !loading,
				colors = androidx.compose.material3.ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.secondary,
					contentColor = MaterialTheme.colorScheme.onSecondary
				)
			) {
				Text("Crear")
			}
		}

		Spacer(Modifier.height(14.dp))
		Card(
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.10f)
			),
			modifier = Modifier.fillMaxWidth()
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(14.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column {
					Text(
						"Restaurantes activos",
						color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
						style = MaterialTheme.typography.bodySmall
					)
					Text(
						total.toString(),
						color = MaterialTheme.colorScheme.onPrimary,
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.Bold
					)
				}
				Text(
					if (loading) "Sincronizando…" else "Toca para abrir un tenant",
					color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
					style = MaterialTheme.typography.bodySmall
				)
			}
		}
	}
}

@Composable
private fun TenantCard(
	t: TenantDto,
	enabled: Boolean,
	onOpen: () -> Unit,
	onToggle: () -> Unit
) {
	val active = t.status == TenantStatus.ACTIVE
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(enabled = enabled, onClick = onOpen),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Row(
				modifier = Modifier.weight(1f),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Box(
					modifier = Modifier
						.size(48.dp)
						.clip(MaterialTheme.shapes.medium)
						.background(MaterialTheme.colorScheme.primaryContainer),
					contentAlignment = Alignment.Center
				) {
					Text(
						t.name.take(2).uppercase(),
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.primary
					)
				}
				Column {
					Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
					Text(t.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
				}
			}

			Column(horizontalAlignment = Alignment.End) {
				Surface(
					color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
					shape = MaterialTheme.shapes.small
				) {
					Text(
						text = if (active) "Activo" else "Inactivo",
						modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
						style = MaterialTheme.typography.labelSmall,
						fontWeight = FontWeight.Bold,
						color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
					)
				}
				Spacer(Modifier.height(6.dp))
				Switch(
					checked = active,
					enabled = enabled,
					onCheckedChange = { onToggle() }
				)
			}
		}
	}
}
