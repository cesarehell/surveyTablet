package com.mr.restaurant.survey.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mr.restaurant.survey.admin.alert.TenantAlertsScreen
import com.mr.restaurant.survey.admin.location.TenantLocationsScreen
import com.mr.restaurant.survey.admin.metrics.TenantMetricsScreen
import com.mr.restaurant.survey.admin.navigation.Routes
import com.mr.restaurant.survey.admin.navigation.requireTemplateId
import com.mr.restaurant.survey.admin.navigation.requireTenantId
import com.mr.restaurant.survey.admin.push.AdminPushViewModel
import com.mr.restaurant.survey.admin.threshold.TenantThresholdsScreen
import com.mr.restaurant.survey.admin.template.CreateTemplateRoute
import com.mr.restaurant.survey.admin.template.TemplateDetailRoute
import com.mr.restaurant.survey.admin.template.TenantTemplateScreen
import com.mr.restaurant.survey.admin.tenant.TenantDashboardScreen
import com.mr.restaurant.survey.admin.tenant.TenantRoute

@Composable
fun AdminNavHost() {
	val nav = rememberNavController()

	NavHost(navController = nav, startDestination = Routes.TENANTS) {
		composable(Routes.TENANTS) {
			TenantRoute(
				onOpenTenant = { tenantId ->
					nav.navigate(Routes.tenantDash(tenantId))
				}
			)
		}

		composable(
			route = Routes.TENANT_DASH,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			val pushVm: AdminPushViewModel = hiltViewModel()
			LaunchedEffect(tenantId) {
				pushVm.bindTenant(tenantId)
			}
			TenantDashboardScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() },
				onMetrics = { nav.navigate(Routes.tenantMetrics(tenantId)) },
				onLocations = { nav.navigate(Routes.tenantLocations(tenantId)) },
				onTemplates = { nav.navigate(Routes.tenantTemplates(tenantId)) },
				onThresholds = { nav.navigate(Routes.tenantThresholds(tenantId)) },
				onAlerts = { nav.navigate(Routes.tenantAlerts(tenantId)) }
			)
		}

		composable(
			route = Routes.TENANT_LOCATIONS,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantLocationsScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
			)
		}

		composable(
			route = Routes.TENANT_TEMPLATES,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantTemplateScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() },
				onCreate = { nav.navigate(Routes.createTemplate(tenantId)) },
				onOpenTemplate = { templateId ->
					nav.navigate(Routes.templateDetail(tenantId, templateId))
				}
			)
		}

		composable(
			route = Routes.CREATE_TEMPLATE,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			CreateTemplateRoute(
				tenantId = tenantId,
				nav = nav,
				onBack = { nav.popBackStack() }
			)
		}

		composable(
			route = Routes.TEMPLATE_DETAIL,
			arguments = listOf(Routes.tenantIdNavArg, Routes.templateIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			val templateId = backStack.arguments?.let { runCatching { it.requireTemplateId() }.getOrNull() }
			if (tenantId.isNullOrBlank() || templateId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}

			TemplateDetailRoute(
				tenantId = tenantId,
				templateId = templateId,
				onBack = { nav.popBackStack() }
			)
		}

		composable(
			route = Routes.TENANT_METRICS,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantMetricsScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
			)
		}

		composable(
			route = Routes.TENANT_THRESHOLDS,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantThresholdsScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
			)
		}

		composable(
			route = Routes.TENANT_ALERTS,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantAlertsScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
			)
		}
	}
}

@Composable
private fun MissingRouteArgScreen(onBack: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text("No se pudo abrir esta pantalla.", style = MaterialTheme.typography.titleMedium)
		TextButton(onClick = onBack) { Text("Volver") }
	}
}
