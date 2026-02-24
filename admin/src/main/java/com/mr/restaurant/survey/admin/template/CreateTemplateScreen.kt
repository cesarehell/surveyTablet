package com.mr.restaurant.survey.admin.template

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.template.dto.TemplateScope

@Composable
fun CreateTemplateScreen(
	tenantId: String,
	loading: Boolean,
	locations: List<LocationDto>,
	error: String?,
	snackbarHostState: SnackbarHostState,
	onBack: () -> Unit,
	onSubmit: (name: String, scope: TemplateScope, locationId: String?, npsEnabled: Boolean) -> Unit
) {
	var name by rememberSaveable { mutableStateOf("") }
	var scope by rememberSaveable { mutableStateOf(TemplateScope.LOCATION) }
	var selectedLocationId by rememberSaveable { mutableStateOf<String?>(null) }
	var npsEnabled by rememberSaveable { mutableStateOf(true) }

	// Validación UI (evita mandar requests inválidos)
	val nameOk = name.isNotBlank()
	val locationOk = scope != TemplateScope.LOCATION || selectedLocationId != null
	val canSubmit = nameOk && locationOk && !loading

	Scaffold(
		snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
		topBar = {
			SimpleTopBar(
				title = "Crear template",
				subtitle = tenantId,
				onBack = onBack
			)
		}
	) { padding ->
		Column(
			modifier = Modifier
				.padding(padding)
				.padding(16.dp)
				.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(14.dp)
		) {

			if (error != null) {
				ErrorBanner(message = error)
			}

			OutlinedTextField(
				value = name,
				onValueChange = { name = it },
				modifier = Modifier.fillMaxWidth(),
				label = { Text("Nombre") },
				singleLine = true,
				isError = !nameOk && name.isNotEmpty(),
				supportingText = {
					if (!nameOk && name.isNotEmpty()) Text("El nombre es obligatorio")
				}
			)

			ScopeSelector(
				value = scope,
				onChange = {
					scope = it
					if (it != TemplateScope.LOCATION) selectedLocationId = null
				}
			)

			if (scope == TemplateScope.LOCATION) {
				LocationSelector(
					locations = locations,
					selectedId = selectedLocationId,
					onSelect = { selectedLocationId = it },
					isError = !locationOk
				)
			}

			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text("NPS habilitado", modifier = Modifier.weight(1f))
				Switch(
					checked = npsEnabled,
					onCheckedChange = { npsEnabled = it }
				)
			}

			if (loading) {
				LinearProgressIndicator(Modifier.fillMaxWidth())
			}

			Spacer(Modifier.height(8.dp))

			Button(
				onClick = { onSubmit(name.trim(), scope, selectedLocationId, npsEnabled) },
				enabled = canSubmit,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("Crear")
			}
		}
	}
}

@Composable
private fun ScopeSelector(
	value: TemplateScope,
	onChange: (TemplateScope) -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text("Alcance", style = MaterialTheme.typography.titleSmall)

		Row(verticalAlignment = Alignment.CenterVertically) {
			RadioButton(
				selected = value == TemplateScope.LOCATION,
				onClick = { onChange(TemplateScope.LOCATION) }
			)
			Text(
				"Sucursal",
				modifier = Modifier
					.clickable { onChange(TemplateScope.LOCATION) }
					.padding(end = 16.dp)
			)

			RadioButton(
				selected = value == TemplateScope.TENANT,
				onClick = { onChange(TemplateScope.TENANT) }
			)
			Text(
				"Franquicia",
				modifier = Modifier.clickable { onChange(TemplateScope.TENANT) }
			)
		}
	}
}

@Composable
private fun LocationSelector(
	locations: List<LocationDto>,
	selectedId: String?,
	onSelect: (String) -> Unit,
	isError: Boolean
) {
	var expanded by remember { mutableStateOf(false) }

	val selectedName = locations.firstOrNull { it.id == selectedId }?.name ?: "Seleccionar sucursal"
	val enabled = locations.isNotEmpty()

	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text("Sucursal", style = MaterialTheme.typography.titleSmall)

		Box(Modifier.fillMaxWidth()) {
			Button(
				onClick = { expanded = true },
				enabled = enabled,
				modifier = Modifier.fillMaxWidth()
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(selectedName)
					Text("▾")
				}
			}

			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false }
			) {
				locations.forEach { loc ->
					DropdownMenuItem(
						text = { Text(loc.name) },
						onClick = {
							onSelect(loc.id)
							expanded = false
						}
					)
				}
			}
		}

		if (isError) {
			Text(
				"Debes seleccionar una sucursal activa",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.error
			)
		} else if (!enabled) {
			Text(
				"No hay sucursales activas para este tenant",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}
