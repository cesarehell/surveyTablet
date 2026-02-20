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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.template.dto.TemplateScope

@Composable
fun CreateTemplateScreen(
	tenantId: String,
	loading: Boolean,
	locations: List<LocationDto>,
	error: String?,
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
private fun SimpleTopBar(
	title: String,
	subtitle: String? = null,
	onBack: () -> Unit
) {
	Surface(shadowElevation = 2.dp) {
		Row(
			Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			TextButton(onClick = onBack) { Text("←") }

			Spacer(Modifier.width(8.dp))

			Column(Modifier.weight(1f)) {
				Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
				if (!subtitle.isNullOrBlank()) {
					Text(subtitle, style = MaterialTheme.typography.bodySmall)
				}
			}
		}
	}
}

@Composable
private fun ErrorBanner(message: String) {
	Surface(
		color = MaterialTheme.colorScheme.errorContainer,
		contentColor = MaterialTheme.colorScheme.onErrorContainer,
		tonalElevation = 2.dp,
		shape = MaterialTheme.shapes.medium
	) {
		Text(
			text = message,
			modifier = Modifier.padding(12.dp),
			style = MaterialTheme.typography.bodyMedium
		)
	}
}

@Composable
private fun ScopeSelector(
	value: TemplateScope,
	onChange: (TemplateScope) -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text("Scope", style = MaterialTheme.typography.titleSmall)

		Row(verticalAlignment = Alignment.CenterVertically) {
			RadioButton(
				selected = value == TemplateScope.LOCATION,
				onClick = { onChange(TemplateScope.LOCATION) }
			)
			Text(
				"LOCATION (por sucursal)",
				modifier = Modifier
					.clickable { onChange(TemplateScope.LOCATION) }
					.padding(end = 16.dp)
			)

			RadioButton(
				selected = value == TemplateScope.TENANT,
				onClick = { onChange(TemplateScope.TENANT) }
			)
			Text(
				"TENANT (franquicia)",
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

		OutlinedTextField(
			value = selectedName,
			onValueChange = {},
			readOnly = true,
			enabled = enabled,
			modifier = Modifier.fillMaxWidth(),
			isError = isError,
			trailingIcon = { Text("▾") },
			supportingText = {
				if (isError) Text("Debes seleccionar una sucursal")
				else if (!enabled) Text("No hay sucursales para este tenant")
			}
		)

		Box(
			Modifier
				.fillMaxWidth()
				.height(0.dp)
		)

		LaunchedEffect(selectedName, enabled) { /* no-op */ }

		Box(
			Modifier
				.fillMaxWidth()
				.height(56.dp)
				.clickable(enabled = enabled) { expanded = true }
		)

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
}