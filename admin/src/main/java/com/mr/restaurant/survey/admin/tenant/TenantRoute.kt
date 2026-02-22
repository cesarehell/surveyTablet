package com.mr.restaurant.survey.admin.tenant

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.ui.AdminUiEvent

@Composable
fun TenantRoute(
	onOpenTenant: (String) -> Unit,
	vm: TenantViewModel = hiltViewModel()
) {
	val state by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(Unit) { vm.load() }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.NavigateToTemplateDetail -> Unit
				AdminUiEvent.CloseDialog -> Unit
			}
		}
	}

	TenantScreen(
		state = state,
		snackbarHostState = snackbarHostState,
		onRetry = vm::load,
		onCreateTenant = vm::create,
		onToggleTenant = vm::toggle,
		onOpenTenant = onOpenTenant
	)
}
