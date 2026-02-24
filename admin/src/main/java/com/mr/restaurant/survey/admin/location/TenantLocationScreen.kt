package com.mr.restaurant.survey.admin.location

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun TenantLocationsScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: LocationsViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	var showCreate by remember { mutableStateOf(false) }
	var showEdit by remember { mutableStateOf(false) }
	var showDeleteConfirm by remember { mutableStateOf(false) }
	var editTargetId by remember { mutableStateOf<String?>(null) }
	var showCodes by remember { mutableStateOf(false) }
	var selectedLocationId by remember { mutableStateOf<String?>(null) }
	var selectedLocationName by remember { mutableStateOf<String?>(null) }
	var selectedPairingCode by rememberSaveable { mutableStateOf<String?>(null) }
	var createName by remember { mutableStateOf("") }
	var createCity by remember { mutableStateOf("") }
	var createBranch by remember { mutableStateOf("") }
	var createCode by remember { mutableStateOf("") }
	var createDialogError by remember { mutableStateOf<String?>(null) }
	var editName by remember { mutableStateOf("") }
	var editCity by remember { mutableStateOf("") }
	var editBranch by remember { mutableStateOf("") }
	var editCode by remember { mutableStateOf("") }
	var editActive by remember { mutableStateOf(true) }
	var editDialogError by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(showCodes, st.pairingCodes) {
		if (showCodes && st.pairingCodes.isNotEmpty()) {
			val available = st.pairingCodes.map { it.code }
			if (selectedPairingCode !in available) {
				selectedPairingCode = available.first()
			}
		}
	}
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
								Spacer(Modifier.height(4.dp))
								FilterChip(
									selected = loc.active,
									onClick = {},
									enabled = false,
									label = { Text(if (loc.active) "Activa" else "Inactiva") }
								)
							}

							Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
								TextButton(
									enabled = !st.loading,
									onClick = {
										selectedLocationId = loc.id
										selectedLocationName = loc.name
										selectedPairingCode = null
										vm.loadPairingCodes(loc.id)
										showCodes = true
									}
								) { Text("Codes") }
								TextButton(
									enabled = !st.loading,
									onClick = {
										editTargetId = loc.id
										editName = loc.name
										editCity = loc.city.orEmpty()
										editBranch = loc.branchName.orEmpty()
										editCode = loc.code.orEmpty()
										editActive = loc.active
										editDialogError = null
										showEdit = true
									}
								) { Text("Editar") }
								TextButton(
									enabled = !st.loading && loc.active,
									onClick = {
										editTargetId = loc.id
										selectedLocationName = loc.name
										showDeleteConfirm = true
									}
								) { Text("Desactivar") }
							}
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

	if (showEdit) {
		val targetId = editTargetId
		AlertDialog(
			onDismissRequest = {
				showEdit = false
				editDialogError = null
			},
			title = { Text("Editar location") },
			text = {
				Column {
					editDialogError?.let {
						ErrorBanner(message = it)
						Spacer(Modifier.height(8.dp))
					}
					OutlinedTextField(
						editName,
						{
							editName = it
							editDialogError = null
						},
						label = { Text("Nombre") },
						singleLine = true
					)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						editCity,
						{
							editCity = it
							editDialogError = null
						},
						label = { Text("Ciudad (opcional)") },
						singleLine = true
					)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						editBranch,
						{
							editBranch = it
							editDialogError = null
						},
						label = { Text("Branch (opcional)") },
						singleLine = true
					)
					Spacer(Modifier.height(8.dp))
					OutlinedTextField(
						editCode,
						{
							editCode = it
							editDialogError = null
						},
						label = { Text("Code (opcional)") },
						singleLine = true
					)
					Spacer(Modifier.height(12.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
					) {
						Text("Activa")
						Switch(checked = editActive, onCheckedChange = { editActive = it })
					}
				}
			},
			confirmButton = {
				Button(
					onClick = {
						val id = targetId ?: return@Button
						editDialogError = null
						vm.updateLocation(
							locationId = id,
							name = editName,
							city = editCity.takeIf { it.isNotBlank() },
							branchName = editBranch.takeIf { it.isNotBlank() },
							code = editCode.takeIf { it.isNotBlank() },
							active = editActive
						)
						showEdit = false
					},
					enabled = editName.isNotBlank() && !st.loading && targetId != null
				) { Text("Guardar") }
			},
			dismissButton = {
				TextButton(onClick = { showEdit = false }) { Text("Cancelar") }
			}
		)
	}

	if (showDeleteConfirm) {
		val targetId = editTargetId
		AlertDialog(
			onDismissRequest = { showDeleteConfirm = false },
			title = { Text("Desactivar location") },
			text = {
				Text("La location ${selectedLocationName ?: ""} se marcará como inactiva (soft delete).")
			},
			confirmButton = {
				Button(
					onClick = {
						val id = targetId ?: return@Button
						vm.softDeleteLocation(id)
						showDeleteConfirm = false
					},
					enabled = !st.loading && targetId != null
				) { Text("Desactivar") }
			},
			dismissButton = {
				TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
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
				selectedPairingCode = null
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
						else -> PairingCodesContent(
							codes = st.pairingCodes.map { it.code },
							selectedCode = selectedPairingCode,
							onSelectCode = { selectedPairingCode = it }
						)
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
						selectedPairingCode = null
						vm.clearPairingCodes()
					}) { Text("Cerrar") }
				}
			)
		}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PairingCodesContent(
	codes: List<String>,
	selectedCode: String?,
	onSelectCode: (String) -> Unit
) {
	val clipboard = LocalClipboardManager.current
	val qrBitmap = remember(selectedCode) {
		selectedCode?.let { generateQrBitmap(it, size = 560) }
	}
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		Text(
			"Usa este código (o QR) para configurar la tablet.",
			style = MaterialTheme.typography.bodySmall
		)
		FlowRow(
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			codes.forEach { code ->
				TextButton(onClick = { onSelectCode(code) }) {
					Text(if (code == selectedCode) "[$code]" else code)
				}
			}
		}
		selectedCode?.let {
			Text("Código seleccionado: $it", style = MaterialTheme.typography.labelLarge)
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Button(onClick = { clipboard.setText(AnnotatedString(it)) }) {
					Text("Copiar código")
				}
				TextButton(onClick = { onSelectCode(it) }) {
					Text("Mantener")
				}
			}
		}
		Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
			if (qrBitmap != null) {
				Image(
					bitmap = qrBitmap.asImageBitmap(),
					contentDescription = "QR pairing code",
					modifier = Modifier.size(260.dp)
				)
			}
		}
	}
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? = runCatching {
	val hints = mapOf(
		EncodeHintType.MARGIN to 1,
		EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
	)
	val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
	Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
		for (x in 0 until size) {
			for (y in 0 until size) {
				setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
			}
		}
	}
}.getOrNull()
