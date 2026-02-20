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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TenantLocationsScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: LocationsViewModel = viewModel()
) {
	val st by vm.state.collectAsState()

	var showCreate by remember { mutableStateOf(false) }
	var showCodes by remember { mutableStateOf(false) }
	var selectedLocationId by remember { mutableStateOf<String?>(null) }
	var selectedLocationName by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(tenantId) { vm.load(tenantId) }

	Column(
		Modifier
			.fillMaxSize()
			.padding(16.dp)
	) {
		Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
			TextButton(onClick = onBack) { Text("← Tenants") }
			Button(onClick = { showCreate = true }) { Text("Agregar location") }
		}

		Spacer(Modifier.height(8.dp))
		Text("Locations de $tenantId", style = MaterialTheme.typography.titleLarge)

		st.error?.let {
			Spacer(Modifier.height(8.dp))
			Text(it, color = MaterialTheme.colorScheme.error)
		}

		Spacer(Modifier.height(12.dp))
		if (st.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

		Spacer(Modifier.height(12.dp))
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

					TextButton(onClick = {
						selectedLocationId = loc.id
						selectedLocationName = loc.name
						vm.loadPairingCodes(loc.id)
						showCodes = true
					}) { Text("Codes") }
				}
				HorizontalDivider()
			}
		}
	}

	// Dialog crear
	if (showCreate) {
		var name by remember { mutableStateOf("") }
		var city by remember { mutableStateOf("") }
		var branch by remember { mutableStateOf("") }
		var code by remember { mutableStateOf("") }

		AlertDialog(
			onDismissRequest = { showCreate = false },
			title = { Text("Crear location") },
			text = {
				Column {
					OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(city, { city = it }, label = { Text("Ciudad (opcional)") }, singleLine = true)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(branch, { branch = it }, label = { Text("Branch (opcional)") }, singleLine = true)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(code, { code = it }, label = { Text("Code (opcional)") }, singleLine = true)
				}
			},
			confirmButton = {
				Button(onClick = {
					vm.create(
						tenantId = tenantId,
						name = name,
						city = city.takeIf { it.isNotBlank() },
						branchName = branch.takeIf { it.isNotBlank() },
						code = code.takeIf { it.isNotBlank() },
						onDone = { showCreate = false }
					)
				}) { Text("Crear") }
			},
			dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancelar") } }
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
					st.pairingCodesError != null -> Text(
						st.pairingCodesError!!,
						color = MaterialTheme.colorScheme.error
					)

					st.pairingCodes.isEmpty() -> Text("No hay códigos (se generarán al solicitar).")
					else -> Column { st.pairingCodes.forEach { Text("• ${it.code}") } }
				}
			},
			confirmButton = {
				TextButton(onClick = {
					showCodes = false
					selectedLocationId = null
					selectedLocationName = null
					vm.clearPairingCodes()
				}) { Text("Cerrar") }
			},
			dismissButton = {
				TextButton(onClick = {
					if (selectedId != null) vm.loadPairingCodes(selectedId)
				}) { Text("Reintentar") }
			}
		)
	}
}
