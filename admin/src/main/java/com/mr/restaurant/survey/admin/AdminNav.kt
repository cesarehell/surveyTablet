package com.mr.restaurant.survey.admin

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mr.restaurant.survey.admin.location.TenantLocationsScreen
import com.mr.restaurant.survey.admin.tenant.TenantDashboardScreen
import com.mr.restaurant.survey.admin.tenant.TenantRoute

object Routes {
	const val TENANTS = "tenants"
	const val TENANT_DASH = "tenant/{tenantId}"
	const val TENANT_LOCATIONS = "tenant/{tenantId}/locations"
	const val TENANT_TEMPLATES = "tenant/{tenantId}/templates"
	const val TENANT_THRESHOLDS = "tenant/{tenantId}/thresholds"
	const val TENANT_ALERTS = "tenant/{tenantId}/alerts"

	fun tenantDash(tenantId: String) = "tenant/$tenantId"
	fun tenantLocations(tenantId: String) = "tenant/$tenantId/locations"
	fun tenantTemplates(tenantId: String) = "tenant/$tenantId/templates"
	fun tenantThresholds(tenantId: String) = "tenant/$tenantId/thresholds"
	fun tenantAlerts(tenantId: String) = "tenant/$tenantId/alerts"
}

@Composable
fun AdminNavHost() {
	val nav = rememberNavController()

	NavHost(navController = nav, startDestination = Routes.TENANTS) {

		composable(Routes.TENANTS) {
			TenantRoute(
				onOpenTenant = { tenantId ->
					nav.navigate(Routes.tenantDash(tenantId.toString()))
				}
			)
		}

		composable(
			route = Routes.TENANT_DASH,
			arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")!!
			TenantDashboardScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() },
				onLocations = { nav.navigate(Routes.tenantLocations(tenantId)) },
				onTemplates = { nav.navigate(Routes.tenantTemplates(tenantId)) },
				onThresholds = { nav.navigate(Routes.tenantThresholds(tenantId)) },
				onAlerts = { nav.navigate(Routes.tenantAlerts(tenantId)) },
			)
		}

		composable(
			route = Routes.TENANT_LOCATIONS,
			arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
		) { backStack ->
			val tenantId = backStack.arguments?.getString("tenantId")!!
			TenantLocationsScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
			)
		}

		composable(Routes.TENANT_TEMPLATES) { /* TemplatesScreen(tenantId=...) */ }
		composable(Routes.TENANT_THRESHOLDS) { /* ThresholdsScreen(tenantId=...) */ }
		composable(Routes.TENANT_ALERTS) { /* AlertsScreen(tenantId=...) */ }
	}
}
