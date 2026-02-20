package com.mr.restaurant.survey.admin.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mr.restaurant.survey.admin.location.LocationsViewModel

@Composable
fun CreateTemplateRoute(
	tenantId: String,
	nav: NavHostController,
	templateVm: TemplateViewModel = viewModel(),
	locationsVm: LocationsViewModel = viewModel(),
	onBack: () -> Unit
) {
	val templateState by templateVm.state.collectAsState()
	val locState by locationsVm.state.collectAsState()

	LaunchedEffect(tenantId) {
		locationsVm.load(tenantId)
	}

	LaunchedEffect(Unit) {
		templateVm.effects.collect { eff ->
			when (eff) {
				is TemplateViewModel.TemplateEffect.Created -> {
					nav.navigate("templateDetail/$tenantId/${eff.templateId}")
				}
			}
		}
	}

	CreateTemplateScreen(
		tenantId = tenantId,
		loading = templateState.loading || locState.loading,
		locations = locState.locations,
		error = templateState.error ?: locState.error,
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