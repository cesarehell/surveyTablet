package com.mr.restaurant.survey.admin.location

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry

@Composable
fun TenantLocationsScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: LocationsViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	var showCreate by remember { mutableStateOf(false) }
	var showCodes by remember { mutableStateOf(false) }
	var selectedLocationId by remember { mutableStateOf<String?>(null) }
	var selectedLocationName by remember { mutableStateOf<String?>(null) }
	var createName by remember { mutableStateOf("") }
	var createCity by remember { mutableStateOf("") }
	var createBranch by remember { mutableStateOf("") }
	var createCode by remember { mutableStateOf("") }
	var createDialogError by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(tenantId) { vm.load(tenantId) }
		LaunchedEffect(Unit) {
			vm.events.collect { event ->
				when (event) {
					is AdminUiEvent.ShowError -> {
						if (showCreate) {
							createDialogError = event.message
						} else {
							snackbarHostState.showSnackbar(event.message)
						}
					}
					is AdminUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
					AdminUiEvent.CloseDialog -> {
						showCreate = false
						createName = ""
						createCity = ""
						createBranch = ""
						createCode = ""
						createDialogError = null
					}

				is AdminUiEvent.NavigateToTemplateDetail -> Unit
			}
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { padding ->
		Column(
			Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(16.dp)
		) {
			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
				TextButton(onClick = onBack) { Text("← Tenants") }
				Button(
					onClick = { showCreate = true },
					enabled = !st.loading
				) { Text("Agregar location") }
			}

			Spacer(Modifier.height(8.dp))
			Text("Locations de $tenantId", style = MaterialTheme.typography.titleLarge)

			st.error?.let {
				Spacer(Modifier.height(8.dp))
				ErrorWithRetry(
					message = it,
					onRetry = { vm.load(tenantId) }
				)
			}

			Spacer(Modifier.height(12.dp))
			if (st.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

			Spacer(Modifier.height(12.dp))
			if (!st.loading && st.locations.isEmpty()) {
				EmptyState(
					message = "No hay locations registradas para este tenant.",
					modifier = Modifier.weight(1f)
				)
			} else {
				LazyColumn {
					items(st.locations, key = { it.id }) { loc ->
						Row(
							Modifier
								.fillMaxWidth()
								.padding(vertical = 10.dp),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							Column(Modifier.weight(1f)) {
								Text(loc.name, style = MaterialTheme.typography.titleMedium)
								Text(loc.id, style = MaterialTheme.typography.bodySmall)
								val meta = listOfNotNull(loc.city, loc.branchName).joinToString(" • ")
								if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
							}

							TextButton(
								enabled = !st.loading,
								onClick = {
									selectedLocationId = loc.id
									selectedLocationName = loc.name
									vm.loadPairingCodes(loc.id)
									showCodes = true
								}
							) { Text("Codes") }
						}
						HorizontalDivider()
					}
				}
			}
		}
	}

	// Dialog crear
	if (showCreate) {
		AlertDialog(
			onDismissRequest = {
				showCreate = false
				createDialogError = null
			},
			title = { Text("Crear location") },
			text = {
				Column {
					createDialogError?.let {
						ErrorBanner(message = it)
						Spacer(Modifier.height(8.dp))
					}
					OutlinedTextField(
						createName,
						{
							createName = it
							createDialogError = null
						},
						label = { Text("Nombre") },
						singleLine = true
					)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						createCity,
						{
							createCity = it
							createDialogError = null
						},
						label = { Text("Ciudad (opcional)") },
						singleLine = true
					)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						createBranch,
						{
							createBranch = it
							createDialogError = null
						},
						label = { Text("Branch (opcional)") },
						singleLine = true
					)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						createCode,
						{
							createCode = it
							createDialogError = null
						},
						label = { Text("Code (opcional)") },
						singleLine = true
					)
				}
			},
			confirmButton = {
				Button(
					onClick = {
						createDialogError = null
						vm.create(
							tenantId = tenantId,
							name = createName,
							city = createCity.takeIf { it.isNotBlank() },
							branchName = createBranch.takeIf { it.isNotBlank() },
							code = createCode.takeIf { it.isNotBlank() }
						)
					},
					enabled = createName.isNotBlank() && !st.loading
				) { Text("Crear") }
			},
			dismissButton = {
				TextButton(onClick = {
					showCreate = false
					createDialogError = null
				}) { Text("Cancelar") }
			}
		)
	}

	if (showCodes) {
		val title = "Pairing codes • ${selectedLocationName ?: ""}".trim()
		val selectedId = selectedLocationId

			AlertDialog(
			onDismissRequest = {
				showCodes = false
				selectedLocationId = null
				selectedLocationName = null
				vm.clearPairingCodes()
			},
			title = { Text(title) },
				text = {
					when {
						st.pairingCodesLoading -> Text("Cargando…")
						st.pairingCodesError != null -> {
							val pairingError = st.pairingCodesError
							if (pairingError != null) {
								Text(
									pairingError,
									color = MaterialTheme.colorScheme.error
								)
							}
						}

						st.pairingCodes.isEmpty() -> Text("No hay códigos (se generarán al solicitar).")
						else -> Column { st.pairingCodes.forEach { Text("• ${it.code}") } }
					}
				},
				confirmButton = {
					if (st.pairingCodesError != null && selectedId != null) {
						TextButton(onClick = { vm.loadPairingCodes(selectedId) }) { Text("Reintentar") }
					}
				},
				dismissButton = {
					TextButton(onClick = {
						showCodes = false
						selectedLocationId = null
						selectedLocationName = null
						vm.clearPairingCodes()
					}) { Text("Cerrar") }
				}
			)
		}
}
