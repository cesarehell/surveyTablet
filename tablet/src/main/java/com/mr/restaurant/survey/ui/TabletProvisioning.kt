package com.mr.restaurant.survey.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mr.restaurant.survey.net.dto.LocationDto
import com.mr.restaurant.survey.net.dto.PairTabletResponseDto
import kotlinx.coroutines.launch

data class TabletProvisioningConfig(
	val tenantId: String,
	val locationId: String,
	val tableNo: String? = null,
	val waiterName: String? = null,
	val waiters: List<String> = emptyList(),
)

class TabletProvisioningPrefs(context: Context) {
	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	fun load(): TabletProvisioningConfig? {
		val tenantId = prefs.getString(KEY_TENANT_ID, null)?.trim().orEmpty()
		val locationId = prefs.getString(KEY_LOCATION_ID, null)?.trim().orEmpty()
		if (tenantId.isBlank() || locationId.isBlank()) return null
		val tableNo = prefs.getString(KEY_TABLE_NO, null)?.trim().orEmpty().ifBlank { null }
		val waiterName = prefs.getString(KEY_WAITER_NAME, null)?.trim().orEmpty().ifBlank { null }
		val waiters = prefs.getString(KEY_WAITERS, null)
			.orEmpty()
			.split(SEP)
			.map { it.trim() }
			.filter { it.isNotBlank() }
			.distinct()
		return TabletProvisioningConfig(
			tenantId = tenantId,
			locationId = locationId,
			tableNo = tableNo,
			waiterName = waiterName,
			waiters = waiters
		)
	}

	fun save(config: TabletProvisioningConfig) {
		prefs.edit()
			.putString(KEY_TENANT_ID, config.tenantId.trim())
			.putString(KEY_LOCATION_ID, config.locationId.trim())
			.putString(KEY_TABLE_NO, config.tableNo?.trim().orEmpty())
			.putString(KEY_WAITER_NAME, config.waiterName?.trim().orEmpty())
			.putString(
				KEY_WAITERS,
				config.waiters.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(SEP)
			)
			.apply()
	}

	fun clear() {
		prefs.edit().clear().apply()
	}

	private companion object {
		const val PREFS_NAME = "tablet_provisioning"
		const val KEY_TENANT_ID = "tenant_id"
		const val KEY_LOCATION_ID = "location_id"
		const val KEY_TABLE_NO = "table_no"
		const val KEY_WAITER_NAME = "waiter_name"
		const val KEY_WAITERS = "waiters"
		const val SEP = "|"
	}
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TabletProvisioningScreen(
	initialConfig: TabletProvisioningConfig? = null,
	defaultTenantId: String,
	baseUrl: String,
	deviceId: String,
	scannedPairingCode: String?,
	onRequestQrScan: () -> Unit,
	onConsumeScannedPairingCode: () -> Unit,
	onLoadLocations: suspend (tenantId: String) -> List<LocationDto>,
	onPairTablet: suspend (pairingCode: String, deviceId: String) -> PairTabletResponseDto,
	onSave: (TabletProvisioningConfig) -> Unit
) {
	val scope = rememberCoroutineScope()
	var pairingCode by rememberSaveable(initialConfig?.tenantId, initialConfig?.locationId) { mutableStateOf("") }
	var pairingLoading by remember { mutableStateOf(false) }
	var pairingError by remember { mutableStateOf<String?>(null) }
	var tenantId by rememberSaveable(initialConfig?.tenantId) {
		mutableStateOf(
			initialConfig?.tenantId ?: defaultTenantId
		)
	}
	var locationIdManual by rememberSaveable(initialConfig?.locationId) { mutableStateOf(initialConfig?.locationId.orEmpty()) }
	var selectedLocationId by rememberSaveable(initialConfig?.locationId) { mutableStateOf(initialConfig?.locationId) }
	var selectedLocationLabel by rememberSaveable(initialConfig?.locationId) { mutableStateOf<String?>(null) }
	var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
	var locationsLoading by remember { mutableStateOf(false) }
	var locationsError by remember { mutableStateOf<String?>(null) }
	var locationsExpanded by remember { mutableStateOf(false) }
	var waitersExpanded by remember { mutableStateOf(false) }
	var manualLocationMode by rememberSaveable { mutableStateOf(false) }
	var operationExpanded by rememberSaveable(initialConfig?.tableNo, initialConfig?.waiterName, initialConfig?.waiters) {
		mutableStateOf(
			!initialConfig?.tableNo.isNullOrBlank() ||
				!initialConfig?.waiterName.isNullOrBlank() ||
				!initialConfig?.waiters.isNullOrEmpty()
		)
	}
	var tableNo by rememberSaveable(initialConfig?.tableNo) { mutableStateOf(initialConfig?.tableNo.orEmpty()) }
	var defaultWaiterName by rememberSaveable(initialConfig?.waiterName) { mutableStateOf(initialConfig?.waiterName.orEmpty()) }
	var waiterDraft by rememberSaveable { mutableStateOf("") }
	var waiters by rememberSaveable(initialConfig?.waiters) { mutableStateOf(initialConfig?.waiters.orEmpty()) }
	var error by remember { mutableStateOf<String?>(null) }

	fun resetLocations() {
		locations = emptyList()
		locationsLoading = false
		locationsError = null
		locationsExpanded = false
		selectedLocationId = null
		selectedLocationLabel = null
	}

	LaunchedEffect(scannedPairingCode) {
		val code = scannedPairingCode?.trim()?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
		pairingCode = code
		pairingError = null
		onConsumeScannedPairingCode()
	}

	fun addWaiter() {
		val normalized = waiterDraft.trim()
		if (normalized.isBlank()) return
		if (waiters.none { it.equals(normalized, ignoreCase = true) }) {
			waiters = (waiters + normalized).sortedBy { it.lowercase() }
		}
		if (defaultWaiterName.isBlank()) {
			defaultWaiterName = normalized
		}
		waiterDraft = ""
	}

	MaterialTheme {
		Surface(Modifier.fillMaxSize()) {
			BoxWithConstraints(Modifier.fillMaxSize()) {
				val cardMaxWidth = if (maxWidth > 840.dp) 760.dp else 680.dp
				Column(
					modifier = Modifier
						.fillMaxSize()
						.verticalScroll(rememberScrollState())
						.padding(20.dp),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Card(
						modifier = Modifier
							.fillMaxWidth()
							.widthIn(max = cardMaxWidth),
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.surface
						)
					) {
						Column(
							modifier = Modifier.padding(18.dp),
							verticalArrangement = Arrangement.spacedBy(14.dp)
						) {
							Text(
								"Configurar tablet",
								style = MaterialTheme.typography.titleLarge
							)
							Text(
								"Ingresa tenant y sucursal para habilitar encuesta, sync y push por location.",
								style = MaterialTheme.typography.bodyMedium
							)
							SectionHeader(
								title = "Emparejamiento recomendado",
								subtitle = "Usa un código o QR desde Admin > Sucursales > Códigos"
							)
							OutlinedTextField(
								value = pairingCode,
								onValueChange = {
									pairingCode = it.uppercase()
									pairingError = null
									error = null
								},
								label = { Text("Pairing code") },
								modifier = Modifier.fillMaxWidth(),
								singleLine = true
							)
							Button(
								onClick = {
									val code = pairingCode.trim()
									if (code.isBlank()) {
										pairingError = "Ingresa un pairing code"
										return@Button
									}
									pairingLoading = true
									pairingError = null
									error = null
									scope.launch {
										runCatching { onPairTablet(code, deviceId) }
											.onSuccess { resp ->
												pairingLoading = false
												val cfg = TabletProvisioningConfig(
													tenantId = resp.tenantId,
													locationId = resp.locationId,
													tableNo = tableNo.trim().ifBlank { null },
													waiterName = defaultWaiterName.trim().ifBlank { null },
													waiters = waiters
												)
												onSave(cfg)
											}
											.onFailure { e ->
												pairingLoading = false
												pairingError = e.message ?: "No se pudo emparejar la tablet"
											}
									}
								},
								enabled = !pairingLoading,
								modifier = Modifier.fillMaxWidth()
							) {
								if (pairingLoading) {
									CircularProgressIndicator(
										modifier = Modifier.height(18.dp),
										strokeWidth = 2.dp
									)
								} else {
									Text("Emparejar y continuar")
								}
							}
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(8.dp)
							) {
								OutlinedButton(
									onClick = onRequestQrScan,
									modifier = Modifier.weight(1f)
								) { Text("Escanear QR") }
								OutlinedButton(
									onClick = { pairingCode = "" },
									modifier = Modifier.weight(1f)
								) { Text("Limpiar código") }
							}
							pairingError?.let {
								Text(
									it,
									color = MaterialTheme.colorScheme.error,
									style = MaterialTheme.typography.bodySmall
								)
							}
							HorizontalDivider()
							SectionHeader(
								title = "Configuración manual",
								subtitle = "Úsala si aún no tienes código de emparejamiento"
							)
							OutlinedTextField(
								value = tenantId,
								onValueChange = {
									tenantId = it
									error = null
									resetLocations()
								},
								label = { Text("Tenant ID") },
								modifier = Modifier.fillMaxWidth(),
								singleLine = true
							)
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(8.dp),
								verticalAlignment = Alignment.CenterVertically
							) {
								Button(
									onClick = {
										val t = tenantId.trim()
										if (t.isBlank()) {
											error = "Ingresa un tenant antes de buscar sucursales"
											return@Button
										}
										error = null
										locationsError = null
										locationsLoading = true
										pairingError = null
										scope.launch {
											runCatching { onLoadLocations(t) }
												.onSuccess { result ->
													val active = result.filter { it.active != false }
													locations = active
													locationsLoading = false
													locationsError =
														if (active.isEmpty()) "No hay sucursales activas para este tenant" else null
													if (active.size == 1) {
														val only = active.first()
														selectedLocationId = only.id
														selectedLocationLabel = formatLocationLabel(only)
														manualLocationMode = false
													} else if (initialConfig?.locationId != null && selectedLocationLabel == null) {
														active.firstOrNull { it.id == initialConfig.locationId }
															?.let { preselected ->
																selectedLocationId = preselected.id
																selectedLocationLabel = formatLocationLabel(preselected)
															}
													}
												}
												.onFailure { e ->
													locations = emptyList()
													locationsLoading = false
													locationsError = e.message ?: "No se pudieron cargar las sucursales"
												}
										}
									},
									enabled = !locationsLoading
								) {
									if (locationsLoading) {
										CircularProgressIndicator(
											modifier = Modifier.height(18.dp),
											strokeWidth = 2.dp
										)
									} else {
										Text("Cargar sucursales")
									}
								}
								OutlinedButton(
									onClick = { manualLocationMode = !manualLocationMode }
								) {
									Text(if (manualLocationMode) "Usar lista" else "ID manual")
								}
							}
							if (!manualLocationMode) {
								Box(modifier = Modifier.fillMaxWidth()) {
									Button(
										onClick = { locationsExpanded = true },
										enabled = locations.isNotEmpty(),
										modifier = Modifier.fillMaxWidth()
									) {
										Text(
											text = selectedLocationLabel ?: "Seleccionar sucursal activa",
											maxLines = 1,
											overflow = TextOverflow.Ellipsis
										)
									}
									DropdownMenu(
										expanded = locationsExpanded,
										onDismissRequest = { locationsExpanded = false }
									) {
										locations.forEach { loc ->
											DropdownMenuItem(
												text = {
													Text(
														formatLocationLabel(loc),
														maxLines = 1,
														overflow = TextOverflow.Ellipsis
													)
												},
												onClick = {
													selectedLocationId = loc.id
													selectedLocationLabel = formatLocationLabel(loc)
													locationsExpanded = false
													error = null
												}
											)
										}
									}
								}
							} else {
								OutlinedTextField(
									value = locationIdManual,
									onValueChange = {
										locationIdManual = it
										error = null
									},
									label = { Text("Location ID (UUID)") },
									modifier = Modifier.fillMaxWidth(),
									singleLine = true
								)
							}
							locationsError?.let {
								Text(
									it,
									color = MaterialTheme.colorScheme.error,
									style = MaterialTheme.typography.bodySmall
								)
							}
							if (error != null) {
								Text(
									error!!,
									color = MaterialTheme.colorScheme.error,
									style = MaterialTheme.typography.bodySmall
								)
							}
							HorizontalDivider()
							Button(
								onClick = {
									val effectiveLocationId = if (manualLocationMode) {
										locationIdManual.trim()
									} else {
										selectedLocationId?.trim().orEmpty()
									}
									val cfg = TabletProvisioningConfig(
										tenantId = tenantId.trim(),
										locationId = effectiveLocationId,
										tableNo = tableNo.trim().ifBlank { null },
										waiterName = defaultWaiterName.trim().ifBlank { null },
										waiters = waiters
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
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.End
							) {
								TextButton(
									onClick = {
										tenantId = defaultTenantId
										pairingCode = ""
										locationIdManual = ""
										tableNo = initialConfig?.tableNo.orEmpty()
										defaultWaiterName = initialConfig?.waiterName.orEmpty()
										waiterDraft = ""
										waiters = initialConfig?.waiters.orEmpty()
										manualLocationMode = false
										resetLocations()
										error = null
									}
								) {
									Text("Restablecer campos")
								}
							}
						}
					}
				}
			}
		}
	}
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
	Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
		Text(title, style = MaterialTheme.typography.titleSmall)
		Text(
			subtitle,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

private fun formatLocationLabel(loc: LocationDto): String {
	val name = loc.name?.takeIf { it.isNotBlank() } ?: loc.id
	val branch = loc.branchName?.takeIf { it.isNotBlank() }
	val city = loc.city?.takeIf { it.isNotBlank() }
	val suffix = listOfNotNull(branch, city).joinToString(" · ")
	return if (suffix.isBlank()) name else "$name · $suffix"
}
