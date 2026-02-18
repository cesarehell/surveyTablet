package com.mr.restaurant.survey.admin.tenant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TenantRoute(
	onOpenTenant: (String) -> Unit,
	vm: TenantViewModel = viewModel()
) {
	val state by vm.state.collectAsState()

	LaunchedEffect(Unit) { vm.load() }

	TenantScreen(
		state = state,
		onCreateTenant = vm::create,
		onToggleTenant = vm::toggle,
		onOpenTenant = onOpenTenant
	)
}
