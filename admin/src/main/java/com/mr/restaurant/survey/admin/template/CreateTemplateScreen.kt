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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.admin.R
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.admin.validation.AdminInputValidator
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
	val nameValidationError = AdminInputValidator.validateName(name)

	// Validación UI (evita mandar requests inválidos)
	val nameOk = nameValidationError == null
	val locationOk = scope != TemplateScope.LOCATION || selectedLocationId != null
	val canSubmit = nameOk && locationOk && !loading

	Scaffold(
		snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
		topBar = {
			SimpleTopBar(
				title = stringResource(R.string.template_create_title),
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
				onValueChange = { name = AdminInputValidator.sanitizeSingleLineInput(it) },
				modifier = Modifier.fillMaxWidth(),
				label = { Text(stringResource(R.string.template_name)) },
				singleLine = true,
				isError = !nameOk && name.isNotEmpty(),
				supportingText = {
					if (!nameOk && name.isNotEmpty()) {
						Text(nameValidationError ?: stringResource(R.string.template_name_required))
					}
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
				Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.spacedBy(2.dp)
				) {
					Text(stringResource(R.string.template_nps_enabled))
					Text(
						stringResource(R.string.template_nps_enabled_help),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
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
				Text(stringResource(R.string.common_create))
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
		Text(stringResource(R.string.template_scope), style = MaterialTheme.typography.titleSmall)

		Row(verticalAlignment = Alignment.CenterVertically) {
			RadioButton(
				selected = value == TemplateScope.LOCATION,
				onClick = { onChange(TemplateScope.LOCATION) }
			)
			Text(
				stringResource(R.string.template_scope_location),
				modifier = Modifier
					.clickable { onChange(TemplateScope.LOCATION) }
					.padding(end = 16.dp)
			)

			RadioButton(
				selected = value == TemplateScope.TENANT,
				onClick = { onChange(TemplateScope.TENANT) }
			)
			Text(
				stringResource(R.string.template_scope_tenant),
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

	val selectedName =
		locations.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.template_location_select)
	val enabled = locations.isNotEmpty()

	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text(stringResource(R.string.template_location_label), style = MaterialTheme.typography.titleSmall)

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
				stringResource(R.string.template_location_required),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.error
			)
		} else if (!enabled) {
			Text(
				stringResource(R.string.template_location_empty),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}
