package com.mr.restaurant.survey.admin.location

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mr.restaurant.survey.admin.R
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.DialogCancelButton
import com.mr.restaurant.survey.admin.ui.DialogConfirmButton
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.ReadOnlyStatusChip
import com.mr.restaurant.survey.core.location.dto.LocationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
	fun closePairingCodesDialog() {
		showCodes = false
		selectedLocationId = null
		selectedLocationName = null
		selectedPairingCode = null
		vm.clearPairingCodes()
	}
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> {
					if (showCreate) {
						createDialogError = event.message
					} else {
						launch { snackbarHostState.showSnackbar(event.message) }
					}
				}

				is AdminUiEvent.ShowSuccess -> launch { snackbarHostState.showSnackbar(event.message) }
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
				) { Text(stringResource(R.string.locations_add)) }
			}

			Spacer(Modifier.height(8.dp))
			Text(stringResource(R.string.locations_title, tenantId), style = MaterialTheme.typography.titleLarge)

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
					message = stringResource(R.string.locations_empty),
					modifier = Modifier.weight(1f)
				)
			} else {
				LazyColumn {
					items(st.locations, key = { it.id }) { loc ->
						LocationListItem(
							location = loc,
							loading = st.loading,
							onCodes = {
								selectedLocationId = loc.id
								selectedLocationName = loc.name
								selectedPairingCode = null
								vm.loadPairingCodes(loc.id)
								showCodes = true
							},
							onEdit = {
								editTargetId = loc.id
								editName = loc.name
								editCity = loc.city.orEmpty()
								editBranch = loc.branchName.orEmpty()
								editCode = loc.code.orEmpty()
								editActive = loc.active
								editDialogError = null
								showEdit = true
							},
							onDeactivate = {
								editTargetId = loc.id
								selectedLocationName = loc.name
								showDeleteConfirm = true
							}
						)
						HorizontalDivider()
					}
				}
			}
		}
	}

	// Dialog crear
	if (showCreate) {
		LocationUpsertDialog(
			title = stringResource(R.string.locations_create_title),
			error = createDialogError,
			name = createName,
			onNameChange = {
				createName = it
				createDialogError = null
			},
			city = createCity,
			onCityChange = {
				createCity = it
				createDialogError = null
			},
			branch = createBranch,
			onBranchChange = {
				createBranch = it
				createDialogError = null
			},
			code = createCode,
			onCodeChange = {
				createCode = it
				createDialogError = null
			},
			showActiveToggle = false,
			active = true,
			onActiveChange = {},
			confirmText = stringResource(R.string.common_create),
			confirmEnabled = createName.isNotBlank() && !st.loading,
			onDismiss = {
				showCreate = false
				createDialogError = null
			},
			onConfirm = {
				createDialogError = null
				vm.create(
					tenantId = tenantId,
					name = createName,
					city = createCity.takeIf { it.isNotBlank() },
					branchName = createBranch.takeIf { it.isNotBlank() },
					code = createCode.takeIf { it.isNotBlank() }
				)
			}
		)
	}

	if (showEdit) {
		val targetId = editTargetId
		LocationUpsertDialog(
			title = stringResource(R.string.locations_edit_title),
			error = editDialogError,
			name = editName,
			onNameChange = {
				editName = it
				editDialogError = null
			},
			city = editCity,
			onCityChange = {
				editCity = it
				editDialogError = null
			},
			branch = editBranch,
			onBranchChange = {
				editBranch = it
				editDialogError = null
			},
			code = editCode,
			onCodeChange = {
				editCode = it
				editDialogError = null
			},
			showActiveToggle = true,
			active = editActive,
			onActiveChange = { editActive = it },
			confirmText = stringResource(R.string.common_save),
			confirmEnabled = editName.isNotBlank() && !st.loading && targetId != null,
			onDismiss = { showEdit = false },
			onConfirm = {
				val id = targetId ?: return@LocationUpsertDialog
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
			}
		)
	}

	if (showDeleteConfirm) {
		val targetId = editTargetId
		AlertDialog(
			onDismissRequest = { showDeleteConfirm = false },
			title = { Text(stringResource(R.string.locations_deactivate_title)) },
			text = {
				Text(stringResource(R.string.locations_deactivate_message, selectedLocationName ?: ""))
			},
			confirmButton = {
				DialogConfirmButton(
					text = stringResource(R.string.common_deactivate),
					enabled = !st.loading && targetId != null,
					onClick = {
						val id = targetId ?: return@DialogConfirmButton
						vm.softDeleteLocation(id)
						showDeleteConfirm = false
					}
				)
			},
			dismissButton = {
				DialogCancelButton(onClick = { showDeleteConfirm = false })
			}
		)
	}

	if (showCodes) {
		val title = stringResource(R.string.locations_pairing_title, selectedLocationName ?: "")
		val selectedId = selectedLocationId

		PairingCodesDialog(
			title = title,
			loading = st.pairingCodesLoading,
			error = st.pairingCodesError,
			codes = st.pairingCodes.map { it.code },
			selectedCode = selectedPairingCode,
			onSelectCode = { selectedPairingCode = it },
			onRetry = if (selectedId != null) ({ vm.loadPairingCodes(selectedId) }) else null,
			onDismiss = ::closePairingCodesDialog
		)
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("DEPRECATION")
private fun PairingCodesContent(
	codes: List<String>,
	selectedCode: String?,
	onSelectCode: (String) -> Unit
) {
	val clipboard = LocalClipboardManager.current
	val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = selectedCode) {
		value = selectedCode?.let { code ->
			withContext(Dispatchers.Default) { generateQrBitmap(code, size = 560) }
		}
	}
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		Text(stringResource(R.string.locations_pairing_help), style = MaterialTheme.typography.bodySmall)
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
			Text(
				stringResource(R.string.locations_pairing_selected_code, it),
				style = MaterialTheme.typography.labelLarge
			)
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Button(onClick = { clipboard.setText(AnnotatedString(it)) }) {
					Text(stringResource(R.string.locations_copy_code))
				}
				TextButton(onClick = { onSelectCode(it) }) {
					Text(stringResource(R.string.locations_keep_code))
				}
			}
		}
		Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
			if (qrBitmap != null) {
				Image(
					bitmap = qrBitmap!!.asImageBitmap(),
					contentDescription = stringResource(R.string.locations_qr_content_description),
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

@Composable
private fun LocationListItem(
	location: LocationDto,
	loading: Boolean,
	onCodes: () -> Unit,
	onEdit: () -> Unit,
	onDeactivate: () -> Unit
) {
	Row(
		Modifier
			.fillMaxWidth()
			.padding(vertical = 10.dp),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Column(Modifier.weight(1f)) {
			Text(location.name, style = MaterialTheme.typography.titleMedium)
			Text(location.id, style = MaterialTheme.typography.bodySmall)
			val meta = listOfNotNull(location.city, location.branchName).joinToString(" • ")
			if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
			Spacer(Modifier.height(4.dp))
			ReadOnlyStatusChip(
				active = location.active,
				activeLabel = stringResource(R.string.common_active),
				inactiveLabel = stringResource(R.string.common_inactive)
			)
		}

		Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
			TextButton(enabled = !loading, onClick = onCodes) { Text(stringResource(R.string.locations_codes)) }
			TextButton(enabled = !loading, onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
			TextButton(enabled = !loading && location.active, onClick = onDeactivate) {
				Text(stringResource(R.string.common_deactivate))
			}
		}
	}
}

@Composable
private fun LocationFormFields(
	name: String,
	onNameChange: (String) -> Unit,
	city: String,
	onCityChange: (String) -> Unit,
	branch: String,
	onBranchChange: (String) -> Unit,
	code: String,
	onCodeChange: (String) -> Unit
) {
	OutlinedTextField(
		value = name,
		onValueChange = onNameChange,
		label = { Text(stringResource(R.string.locations_name)) },
		singleLine = true
	)
	Spacer(Modifier.height(8.dp))
	OutlinedTextField(
		value = city,
		onValueChange = onCityChange,
		label = { Text(stringResource(R.string.locations_city_optional)) },
		singleLine = true
	)
	Spacer(Modifier.height(8.dp))
	OutlinedTextField(
		value = branch,
		onValueChange = onBranchChange,
		label = { Text(stringResource(R.string.locations_branch_optional)) },
		singleLine = true
	)
	Spacer(Modifier.height(8.dp))
	OutlinedTextField(
		value = code,
		onValueChange = onCodeChange,
		label = { Text(stringResource(R.string.locations_code_optional)) },
		singleLine = true
	)
}

@Composable
private fun LocationUpsertDialog(
	title: String,
	error: String?,
	name: String,
	onNameChange: (String) -> Unit,
	city: String,
	onCityChange: (String) -> Unit,
	branch: String,
	onBranchChange: (String) -> Unit,
	code: String,
	onCodeChange: (String) -> Unit,
	showActiveToggle: Boolean,
	active: Boolean,
	onActiveChange: (Boolean) -> Unit,
	confirmText: String,
	confirmEnabled: Boolean,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column {
				error?.let {
					ErrorBanner(message = it)
					Spacer(Modifier.height(8.dp))
				}
				LocationFormFields(
					name = name,
					onNameChange = onNameChange,
					city = city,
					onCityChange = onCityChange,
					branch = branch,
					onBranchChange = onBranchChange,
					code = code,
					onCodeChange = onCodeChange
				)
				if (showActiveToggle) {
					Spacer(Modifier.height(12.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
					) {
						Text(stringResource(R.string.common_active))
						Switch(checked = active, onCheckedChange = onActiveChange)
					}
				}
			}
		},
		confirmButton = {
			DialogConfirmButton(text = confirmText, enabled = confirmEnabled, onClick = onConfirm)
		},
		dismissButton = {
			DialogCancelButton(onClick = onDismiss)
		}
	)
}

@Composable
private fun PairingCodesDialog(
	title: String,
	loading: Boolean,
	error: String?,
	codes: List<String>,
	selectedCode: String?,
	onSelectCode: (String) -> Unit,
	onRetry: (() -> Unit)?,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			when {
				loading -> Text(stringResource(R.string.locations_pairing_loading))
				error != null -> Text(error, color = MaterialTheme.colorScheme.error)
				codes.isEmpty() -> Text(stringResource(R.string.locations_pairing_empty))
				else -> PairingCodesContent(
					codes = codes,
					selectedCode = selectedCode,
					onSelectCode = onSelectCode
				)
			}
		},
		confirmButton = {
			if (error != null && onRetry != null) {
				TextButton(onClick = onRetry) { Text(stringResource(R.string.common_refresh)) }
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
		}
	)
}
