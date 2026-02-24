package com.mr.restaurant.survey.admin.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.core.device.api.DeviceApi
import com.mr.restaurant.survey.core.device.dto.DeviceViewDto
import com.mr.restaurant.survey.core.device.dto.UpdateDeviceAssignmentRequest
import com.mr.restaurant.survey.core.net.ApiResult
import com.mr.restaurant.survey.core.net.safeCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
	val loading: Boolean = false,
	val items: List<DeviceViewDto> = emptyList(),
	val error: String? = null,
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
	private val api: DeviceApi
) : ViewModel() {
	private val _state = MutableStateFlow(DevicesUiState())
	val state = _state.asStateFlow()
	private val _events = MutableSharedFlow<AdminUiEvent>(extraBufferCapacity = 8)
	val events = _events.asSharedFlow()

	fun load(tenantId: String) = viewModelScope.launch {
		_state.update { it.copy(loading = true, error = null) }
		when (val res = safeCall { api.list(tenantId = tenantId) }) {
			is ApiResult.Ok -> _state.update { it.copy(loading = false, items = res.value) }
			is ApiResult.Err -> _state.update { it.copy(loading = false, error = res.message) }
		}
	}

	fun updateAssignment(
		tenantId: String,
		deviceId: String,
		label: String?,
		waiter: String?,
		tableNo: String?,
		active: Boolean
	) = viewModelScope.launch {
		_state.update { it.copy(loading = true) }
		when (val res = safeCall {
			api.update(
				id = deviceId,
				tenantId = tenantId,
				req = UpdateDeviceAssignmentRequest(
					label = label?.trim().takeUnless { it.isNullOrBlank() },
					assignedWaiterName = waiter?.trim().takeUnless { it.isNullOrBlank() },
					defaultTableNo = tableNo?.trim().takeUnless { it.isNullOrBlank() },
					active = active
				)
			)
		}) {
			is ApiResult.Ok -> {
				_state.update { st ->
					st.copy(
						loading = false,
						items = st.items.map { if (it.id == deviceId) res.value else it }
					)
				}
				_events.tryEmit(AdminUiEvent.ShowSuccess("Tablet actualizada"))
			}
			is ApiResult.Err -> {
				_state.update { it.copy(loading = false) }
				_events.tryEmit(AdminUiEvent.ShowError(res.message))
			}
		}
	}
}

@Composable
fun TenantDevicesScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: DevicesViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }
	var editTarget by remember { mutableStateOf<DeviceViewDto?>(null) }

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
				else -> Unit
			}
		}
	}

	Scaffold(
		topBar = { SimpleTopBar(title = "Tablets", subtitle = tenantId, onBack = onBack) },
		snackbarHost = { SnackbarHost(snackbarHostState) }
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text("Dispositivos registrados", fontWeight = FontWeight.SemiBold)
				TextButton(onClick = { vm.load(tenantId) }, enabled = !st.loading) { Text("Actualizar") }
			}

			st.error?.let {
				ErrorWithRetry(message = it, onRetry = { vm.load(tenantId) })
			}

			if (st.loading && st.items.isEmpty()) {
				Spacer(Modifier.weight(1f))
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
					CircularProgressIndicator()
				}
				Spacer(Modifier.weight(1f))
			} else if (!st.loading && st.items.isEmpty()) {
				EmptyState("No hay tablets registradas para este tenant.")
			} else {
				LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
					items(st.items, key = { it.id }) { device ->
						DeviceCard(
							device = device,
							onEdit = { editTarget = device }
						)
					}
				}
			}
		}
	}

	editTarget?.let { device ->
		EditDeviceDialog(
			device = device,
			onDismiss = { editTarget = null },
			onSave = { label, waiter, tableNo, active ->
				vm.updateAssignment(
					tenantId = tenantId,
					deviceId = device.id,
					label = label,
					waiter = waiter,
					tableNo = tableNo,
					active = active
				)
				editTarget = null
			}
		)
	}
}

@Composable
private fun DeviceCard(
	device: DeviceViewDto,
	onEdit: () -> Unit
) {
	Card(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = device.label ?: device.owner ?: "Tablet",
					fontWeight = FontWeight.SemiBold,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				FilterChip(
					selected = device.active,
					onClick = {},
					enabled = false,
					label = { Text(if (device.active) "Activa" else "Inactiva") }
				)
			}
			Text("Mesero: ${device.assignedWaiterName ?: "-"}")
			Text("Mesa: ${device.defaultTableNo ?: "-"}")
			Text("Sucursal: ${device.locationId ?: "-"}", maxLines = 1, overflow = TextOverflow.Ellipsis)
			Text("Owner: ${device.owner ?: "-"} • Token: …${device.tokenSuffix ?: ""}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End
			) {
				TextButton(onClick = onEdit) { Text("Editar") }
			}
		}
	}
}

@Composable
private fun EditDeviceDialog(
	device: DeviceViewDto,
	onDismiss: () -> Unit,
	onSave: (label: String?, waiter: String?, tableNo: String?, active: Boolean) -> Unit
) {
	var label by remember(device.id) { mutableStateOf(device.label.orEmpty()) }
	var waiter by remember(device.id) { mutableStateOf(device.assignedWaiterName.orEmpty()) }
	var tableNo by remember(device.id) { mutableStateOf(device.defaultTableNo.orEmpty()) }
	var active by remember(device.id) { mutableStateOf(device.active) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Asignar tablet") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				OutlinedTextField(
					value = label,
					onValueChange = { label = it },
					label = { Text("Alias") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				OutlinedTextField(
					value = waiter,
					onValueChange = { waiter = it },
					label = { Text("Mesero asignado") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				OutlinedTextField(
					value = tableNo,
					onValueChange = { tableNo = it },
					label = { Text("Mesa por defecto") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text("Activa")
					Switch(checked = active, onCheckedChange = { active = it })
				}
			}
		},
		confirmButton = {
			Button(onClick = { onSave(label, waiter, tableNo, active) }) { Text("Guardar") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancelar") }
		}
	)
}
