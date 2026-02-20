package com.mr.restaurant.survey.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mr.restaurant.survey.admin.location.TenantLocationsScreen
import com.mr.restaurant.survey.admin.template.CreateTemplateRoute
import com.mr.restaurant.survey.admin.template.TemplateDetailRoute
import com.mr.restaurant.survey.admin.template.TenantTemplateScreen
import com.mr.restaurant.survey.admin.tenant.TenantDashboardScreen
import com.mr.restaurant.survey.admin.tenant.TenantRoute

object Routes {
	const val TENANTS = "tenants"
	const val TENANT_DASH = "tenant/{tenantId}"
	const val TENANT_LOCATIONS = "tenant/{tenantId}/locations"
	const val TENANT_TEMPLATES = "tenant/{tenantId}/templates"
	const val TENANT_THRESHOLDS = "tenant/{tenantId}/thresholds"
	const val TENANT_ALERTS = "tenant/{tenantId}/alerts"

	const val CREATE_TEMPLATE = "createTemplate/{tenantId}"
	const val TEMPLATE_DETAIL = "templateDetail/{tenantId}/{templateId}"

	fun tenantDash(tenantId: String) = "tenant/$tenantId"
	fun tenantLocations(tenantId: String) = "tenant/$tenantId/locations"
	fun tenantTemplates(tenantId: String) = "tenant/$tenantId/templates"
	fun tenantThresholds(tenantId: String) = "tenant/$tenantId/thresholds"
	fun tenantAlerts(tenantId: String) = "tenant/$tenantId/alerts"

	fun createTemplate(tenantId: String) = "createTemplate/$tenantId"
	fun templateDetail(tenantId: String, templateId: String) = "templateDetail/$tenantId/$templateId"
}

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
			arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantDashboardScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() },
				onLocations = { nav.navigate(Routes.tenantLocations(tenantId)) },
				onTemplates = { nav.navigate(Routes.tenantTemplates(tenantId)) },
				onThresholds = { nav.navigate(Routes.tenantThresholds(tenantId)) },
				onAlerts = { nav.navigate(Routes.tenantAlerts(tenantId)) }
			)
		}

		composable(
			route = Routes.TENANT_LOCATIONS,
			arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")
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
			arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantTemplateScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() },
				onCreate = { nav.navigate(Routes.createTemplate(tenantId)) }
			)
		}

		composable(
			route = Routes.CREATE_TEMPLATE,
			arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")
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
			arguments = listOf(
				navArgument("tenantId") { type = NavType.StringType },
				navArgument("templateId") { type = NavType.StringType }
			)
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")
			val templateId = backStack.arguments?.getString("templateId")
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

		composable(Routes.TENANT_THRESHOLDS) { /* TODO */ }
		composable(Routes.TENANT_ALERTS) { /* TODO */ }
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
