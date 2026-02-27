package com.mr.restaurant.survey.admin.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
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
	var query by remember { mutableStateOf("") }
	var filter by remember { mutableStateOf(TenantListFilter.ALL) }
	val visibleTenants = state.tenants
		.filter {
			when (filter) {
				TenantListFilter.ALL -> true
				TenantListFilter.ACTIVE -> it.status == TenantStatus.ACTIVE
				TenantListFilter.INACTIVE -> it.status == TenantStatus.INACTIVE
			}
		}
		.filter {
			val q = query.trim().lowercase()
			if (q.isBlank()) true
			else it.name.lowercase().contains(q) || it.id.lowercase().contains(q)
		}
		.sortedWith(
			compareBy<com.mr.restaurant.survey.core.tenant.dto.TenantDto>(
				{ if (it.status == TenantStatus.ACTIVE) 0 else 1 },
				{ it.name.lowercase() }
			)
		)

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		containerColor = MaterialTheme.colorScheme.background,
		floatingActionButton = {
			FloatingActionButton(
				onClick = { showCreate = true },
				containerColor = MaterialTheme.colorScheme.primary,
				contentColor = MaterialTheme.colorScheme.onPrimary
			) {
				Icon(Icons.Outlined.Add, contentDescription = "Crear tenant")
			}
		}
	) { padding ->
		Column(
			Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			TenantsHeader()

			Column(
				Modifier
					.fillMaxSize()
					.padding(horizontal = 16.dp)
			) {
				Spacer(Modifier.height(10.dp))
				SearchBar(
					query = query,
					onQueryChange = { query = it }
				)
				Spacer(Modifier.height(12.dp))
				TenantFilterChips(
					selected = filter,
					activeCount = state.tenants.count { it.status == TenantStatus.ACTIVE },
					inactiveCount = state.tenants.count { it.status == TenantStatus.INACTIVE },
					onSelect = { filter = it }
				)

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
				} else if (!state.loading && visibleTenants.isEmpty()) {
					EmptyState(
						message = "No hay resultados para ese filtro.",
						modifier = Modifier.weight(1f)
					)
				} else {
					LazyColumn(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {
						items(visibleTenants, key = { it.id }) { t ->
							TenantCard(
								t = t,
								enabled = !state.loading,
								onOpen = { onOpenTenant(t.id) },
								onToggle = { onToggleTenant(t) }
							)
						}
						item { Spacer(Modifier.height(90.dp)) }
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
private fun TenantsHeader() {
	Text(
		"Tenants",
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp),
		style = MaterialTheme.typography.headlineLarge,
		fontWeight = FontWeight.Bold
	)
}

@Composable
private fun SearchBar(
	query: String,
	onQueryChange: (String) -> Unit
) {
	OutlinedTextField(
		value = query,
		onValueChange = onQueryChange,
		modifier = Modifier.fillMaxWidth(),
		singleLine = true,
		leadingIcon = {
			Icon(Icons.Outlined.Search, contentDescription = null)
		},
		label = { Text("Buscar restaurantes…") }
	)
}

private enum class TenantListFilter { ALL, ACTIVE, INACTIVE }

@Composable
private fun TenantFilterChips(
	selected: TenantListFilter,
	activeCount: Int,
	inactiveCount: Int,
	onSelect: (TenantListFilter) -> Unit
) {
	FlowRow(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		FilterChip(
			selected = selected == TenantListFilter.ALL,
			onClick = { onSelect(TenantListFilter.ALL) },
			label = { Text("Todos") }
		)
		FilterChip(
			selected = selected == TenantListFilter.ACTIVE,
			onClick = { onSelect(TenantListFilter.ACTIVE) },
			label = { Text("Activos ($activeCount)") }
		)
		FilterChip(
			selected = selected == TenantListFilter.INACTIVE,
			onClick = { onSelect(TenantListFilter.INACTIVE) },
			label = { Text("Inactivos ($inactiveCount)") }
		)
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
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
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
							.size(54.dp)
							.clip(MaterialTheme.shapes.large)
							.background(
								if (active) MaterialTheme.colorScheme.primaryContainer
								else MaterialTheme.colorScheme.surfaceVariant
							),
						contentAlignment = Alignment.Center
					) {
						Icon(
							Icons.Outlined.Storefront,
							contentDescription = null,
							tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
					Column {
						Text(
							t.name,
							style = MaterialTheme.typography.titleLarge,
							fontWeight = FontWeight.Bold,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
						Text(
							t.id.uppercase(),
							style = MaterialTheme.typography.titleSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis
						)
					}
				}

				TenantStatusPill(active = active)
			}

			androidx.compose.material3.HorizontalDivider()

			BoxWithConstraints(Modifier.fillMaxWidth()) {
				val compact = maxWidth < 420.dp
				if (compact) {
					Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
						TenantMetaRow(
							leftTitle = "ESTADO",
							leftValue = if (active) "Activo" else "Inactivo",
							rightTitle = "ACCESO",
							rightValue = if (enabled) "Disponible" else "Bloqueado"
						)
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically
						) {
							TenantToggleRow(
								active = active,
								enabled = enabled,
								onToggle = onToggle,
								modifier = Modifier.fillMaxWidth()
							)
						}
					}
				} else {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						TenantMetaRow(
							modifier = Modifier.weight(1f),
							leftTitle = "ESTADO",
							leftValue = if (active) "Activo" else "Inactivo",
							rightTitle = "ACCESO",
							rightValue = if (enabled) "Disponible" else "Bloqueado"
						)
						TenantToggleRow(
							active = active,
							enabled = enabled,
							onToggle = onToggle
						)
					}
				}
			}
		}
	}
}

@Composable
private fun tenantSwitchColors() = SwitchDefaults.colors(
	checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
	checkedTrackColor = MaterialTheme.colorScheme.primary,
	uncheckedThumbColor = MaterialTheme.colorScheme.surface,
	uncheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
	uncheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
	disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
	disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
	disabledUncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
	disabledUncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
)

@Composable
private fun TenantToggleRow(
	active: Boolean,
	enabled: Boolean,
	onToggle: () -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		shape = RoundedCornerShape(14.dp),
		color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
		modifier = modifier.clip(RoundedCornerShape(14.dp))
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 10.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column {
				Text(
					if (active) "Activo" else "Inactivo",
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.SemiBold
				)
				Text(
					"Cambiar estado",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			Switch(
				checked = active,
				enabled = enabled,
				onCheckedChange = { onToggle() },
				colors = tenantSwitchColors()
			)
		}
	}
}

@Composable
private fun TenantStatusPill(active: Boolean) {
	Surface(
		color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
		shape = MaterialTheme.shapes.large
	) {
		Text(
			text = if (active) "ACTIVE" else "INACTIVE",
			modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
			style = MaterialTheme.typography.labelMedium,
			fontWeight = FontWeight.Bold,
			color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Composable
private fun TenantMetaRow(
	modifier: Modifier = Modifier,
	leftTitle: String,
	leftValue: String,
	rightTitle: String,
	rightValue: String
) {
	Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Column {
			Text(leftTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			Text(leftValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
		}
		Spacer(Modifier.width(16.dp))
		Column(horizontalAlignment = Alignment.End) {
			Text(rightTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
			Text(rightValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
		}
	}
}
