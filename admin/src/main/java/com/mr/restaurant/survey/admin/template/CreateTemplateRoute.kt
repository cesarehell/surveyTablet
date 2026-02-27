package com.mr.restaurant.survey.admin.template

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mr.restaurant.survey.admin.location.LocationsViewModel
import com.mr.restaurant.survey.admin.navigation.Routes
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import kotlinx.coroutines.launch

@Composable
fun CreateTemplateRoute(
	tenantId: String,
	nav: NavHostController,
	templateVm: TemplateViewModel = hiltViewModel(),
	locationsVm: LocationsViewModel = hiltViewModel(),
	onBack: () -> Unit
) {
	val templateState by templateVm.state.collectAsState()
	val locState by locationsVm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(tenantId) {
		locationsVm.load(tenantId)
	}

	LaunchedEffect(Unit) {
		templateVm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> launch { snackbarHostState.showSnackbar(event.message) }
				is AdminUiEvent.ShowSuccess -> launch { snackbarHostState.showSnackbar(event.message) }
				is AdminUiEvent.NavigateToTemplateDetail -> {
					nav.navigate(Routes.templateDetail(tenantId, event.templateId))
				}

				AdminUiEvent.CloseDialog -> Unit
			}
		}
	}

	CreateTemplateScreen(
		tenantId = tenantId,
		loading = templateState.loading || locState.loading,
		locations = locState.locations.filter { it.active },
		error = templateState.error ?: locState.error,
		snackbarHostState = snackbarHostState,
		onBack = onBack,
		onSubmit = { name, scope, locationId, npsEnabled ->
			templateVm.create(
				tenantId = tenantId,
				name = name,
				scope = scope,
				locationId = locationId,
				npsEnabled = npsEnabled
			)
		}
	)
}
