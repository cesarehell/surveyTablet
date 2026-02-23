package com.mr.restaurant.survey.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class TabletProvisioningConfig(
	val tenantId: String,
	val locationId: String,
)

class TabletProvisioningPrefs(context: Context) {
	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	fun load(): TabletProvisioningConfig? {
		val tenantId = prefs.getString(KEY_TENANT_ID, null)?.trim().orEmpty()
		val locationId = prefs.getString(KEY_LOCATION_ID, null)?.trim().orEmpty()
		if (tenantId.isBlank() || locationId.isBlank()) return null
		return TabletProvisioningConfig(tenantId = tenantId, locationId = locationId)
	}

	fun save(config: TabletProvisioningConfig) {
		prefs.edit()
			.putString(KEY_TENANT_ID, config.tenantId.trim())
			.putString(KEY_LOCATION_ID, config.locationId.trim())
			.apply()
	}

	fun clear() {
		prefs.edit().clear().apply()
	}

	private companion object {
		const val PREFS_NAME = "tablet_provisioning"
		const val KEY_TENANT_ID = "tenant_id"
		const val KEY_LOCATION_ID = "location_id"
	}
}

@Composable
fun TabletProvisioningScreen(
	defaultTenantId: String,
	baseUrl: String,
	onSave: (TabletProvisioningConfig) -> Unit
) {
	var tenantId by rememberSaveable { mutableStateOf(defaultTenantId) }
	var locationId by rememberSaveable { mutableStateOf("") }
	var error by remember { mutableStateOf<String?>(null) }

	MaterialTheme {
		Surface(Modifier.fillMaxSize()) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(20.dp),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Card(modifier = Modifier.fillMaxWidth()) {
					Column(
						modifier = Modifier.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						Text(
							"Configurar tablet",
							style = MaterialTheme.typography.titleLarge
						)
						Text(
							"Ingresa tenant y sucursal para habilitar encuesta, sync y push por location.",
							style = MaterialTheme.typography.bodyMedium
						)
						Text(
							"Backend: $baseUrl",
							style = MaterialTheme.typography.bodySmall
						)
						OutlinedTextField(
							value = tenantId,
							onValueChange = {
								tenantId = it
								error = null
							},
							label = { Text("Tenant ID") },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true
						)
						OutlinedTextField(
							value = locationId,
							onValueChange = {
								locationId = it
								error = null
							},
							label = { Text("Location ID") },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true
						)
						if (error != null) {
							Text(
								error!!,
								color = MaterialTheme.colorScheme.error,
								style = MaterialTheme.typography.bodySmall
							)
						}
						Button(
							onClick = {
								val cfg = TabletProvisioningConfig(
									tenantId = tenantId.trim(),
									locationId = locationId.trim()
								)
								if (cfg.tenantId.isBlank() || cfg.locationId.isBlank()) {
									error = "Tenant y Location son obligatorios"
								} else {
									onSave(cfg)
								}
							},
							modifier = Modifier.fillMaxWidth()
						) {
							Text("Guardar y continuar")
						}
						Spacer(Modifier.height(2.dp))
						TextButton(
							onClick = {
								tenantId = defaultTenantId
								locationId = ""
								error = null
							},
							modifier = Modifier.align(Alignment.End)
						) {
							Text("Limpiar")
						}
					}
				}
			}
		}
	}
}
