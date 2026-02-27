package com.mr.restaurant.survey.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import com.mr.restaurant.survey.admin.alert.TenantAlertsScreen
import com.mr.restaurant.survey.admin.coupon.TenantCouponRulesScreen
import com.mr.restaurant.survey.admin.device.TenantDevicesScreen
import com.mr.restaurant.survey.admin.location.TenantLocationsScreen
import com.mr.restaurant.survey.admin.metrics.TenantMetricsScreen
import com.mr.restaurant.survey.admin.navigation.Routes
import com.mr.restaurant.survey.admin.navigation.requireTemplateId
import com.mr.restaurant.survey.admin.navigation.requireTenantId
import com.mr.restaurant.survey.admin.push.AdminPushDebugRoute
import com.mr.restaurant.survey.admin.push.AdminPushViewModel
import com.mr.restaurant.survey.admin.template.CreateTemplateRoute
import com.mr.restaurant.survey.admin.template.TemplateDetailRoute
import com.mr.restaurant.survey.admin.template.TenantTemplateScreen
import com.mr.restaurant.survey.admin.tenant.TenantDashboardScreen
import com.mr.restaurant.survey.admin.tenant.TenantRoute
import com.mr.restaurant.survey.admin.threshold.TenantThresholdsScreen

@Composable
fun AdminNavHost() {
	val nav = rememberNavController()
	val backStackEntry by nav.currentBackStackEntryAsState()
	val destination = backStackEntry?.destination
	val currentTenantId = backStackEntry?.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }

	Scaffold(
		bottomBar = {
			val tenantId = currentTenantId
			if (tenantId != null && shouldShowTenantBottomBar(destination)) {
				TenantBottomBar(
					route = destination?.route,
					onHome = { nav.navigateTenantRoot(Routes.tenantDash(tenantId), tenantId) },
					onMetrics = { nav.navigateTenantRoot(Routes.tenantMetrics(tenantId), tenantId) },
					onSurveys = { nav.navigateTenantRoot(Routes.tenantTemplates(tenantId), tenantId) },
					onSettings = { nav.navigateTenantRoot(Routes.tenantDevices(tenantId), tenantId) }
				)
			}
		}
	) { padding ->
		NavHost(
			navController = nav,
			startDestination = Routes.TENANTS,
			modifier = Modifier.padding(padding)
		) {
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
					onDevices = { nav.navigate(Routes.tenantDevices(tenantId)) },
					onTemplates = { nav.navigate(Routes.tenantTemplates(tenantId)) },
					onThresholds = { nav.navigate(Routes.tenantThresholds(tenantId)) },
					onCoupons = { nav.navigate(Routes.tenantCoupons(tenantId)) },
					onAlerts = { nav.navigate(Routes.tenantAlerts(tenantId)) },
					onPushDebug = { nav.navigate(Routes.tenantPushDebug(tenantId)) }
				)
			}

			composable(
			route = Routes.TENANT_DEVICES,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantDevicesScreen(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
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
			route = Routes.TENANT_COUPONS,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			TenantCouponRulesScreen(
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

			composable(
			route = Routes.TENANT_PUSH_DEBUG,
			arguments = listOf(Routes.tenantIdNavArg)
		) { backStack ->
			val tenantId = backStack.arguments?.let { runCatching { it.requireTenantId() }.getOrNull() }
			if (tenantId.isNullOrBlank()) {
				MissingRouteArgScreen(onBack = { nav.popBackStack() })
				return@composable
			}
			AdminPushDebugRoute(
				tenantId = tenantId,
				onBack = { nav.popBackStack() }
			)
			}
		}
	}
}

private fun shouldShowTenantBottomBar(destination: NavDestination?): Boolean {
	val route = destination?.route ?: return false
	return when (route) {
		Routes.TENANT_DASH,
		Routes.TENANT_METRICS,
		Routes.TENANT_TEMPLATES,
		Routes.TENANT_DEVICES,
		Routes.TENANT_LOCATIONS,
		Routes.TENANT_THRESHOLDS,
		Routes.TENANT_COUPONS,
		Routes.TENANT_ALERTS -> true

		else -> false
	}
}

@Composable
private fun TenantBottomBar(
	route: String?,
	onHome: () -> Unit,
	onMetrics: () -> Unit,
	onSurveys: () -> Unit,
	onSettings: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 12.dp, vertical = 10.dp)
			.widthIn(max = 700.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
	) {
		Column(modifier = Modifier.fillMaxWidth()) {
			androidx.compose.foundation.layout.Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 8.dp, vertical = 6.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				BottomMenuItem(
					title = "Inicio",
					icon = Icons.Outlined.Home,
					active = route == Routes.TENANT_DASH,
					onClick = onHome
				)
				BottomMenuItem(
					title = "Métricas",
					icon = Icons.Outlined.Insights,
					active = route == Routes.TENANT_METRICS,
					onClick = onMetrics
				)
				BottomMenuItem(
					title = "Encuestas",
					icon = Icons.Outlined.Description,
					active = route == Routes.TENANT_TEMPLATES,
					onClick = onSurveys
				)
				BottomMenuItem(
					title = "Ajustes",
					icon = Icons.Outlined.Settings,
					active = route == Routes.TENANT_DEVICES,
					onClick = onSettings
				)
			}
		}
	}
}

@Composable
private fun BottomMenuItem(
	title: String,
	icon: ImageVector,
	active: Boolean,
	onClick: () -> Unit
) {
	val activeColor = MaterialTheme.colorScheme.primary
	val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
	val color = if (active) activeColor else inactiveColor
	Column(
		modifier = Modifier
			.clip(MaterialTheme.shapes.medium)
			.then(Modifier)
			.padding(horizontal = 6.dp, vertical = 4.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Box(
			modifier = Modifier
				.size(24.dp)
				.clip(MaterialTheme.shapes.small)
				.background(
					if (active) activeColor.copy(alpha = 0.16f)
					else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
				),
			contentAlignment = Alignment.Center
		) {
			Icon(
				imageVector = icon,
				contentDescription = title,
				tint = color,
				modifier = Modifier.size(16.dp)
			)
		}
		TextButton(onClick = onClick) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Text(
					title,
					color = color,
					style = MaterialTheme.typography.labelMedium,
					fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
				)
				Box(
					modifier = Modifier
						.padding(top = 2.dp)
						.size(if (active) 5.dp else 0.dp)
						.clip(MaterialTheme.shapes.small)
						.background(activeColor.copy(alpha = if (active) 0.95f else 0f))
				)
			}
		}
	}
}

private fun androidx.navigation.NavHostController.navigateTenantRoot(route: String, tenantId: String) {
	navigate(route) {
		popUpTo(Routes.tenantDash(tenantId)) {
			saveState = true
		}
		launchSingleTop = true
		restoreState = true
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
