package com.mr.restaurant.survey.admin.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

object Routes {
	const val TENANTS = "tenants"

	const val TENANT_ID_ARG = "tenantId"

	const val TENANT_DASH = "tenant/{$TENANT_ID_ARG}"
	const val TENANT_LOCATIONS = "tenant/{$TENANT_ID_ARG}/locations"
	const val TENANT_TEMPLATES = "tenant/{$TENANT_ID_ARG}/templates"
	const val TENANT_THRESHOLDS = "tenant/{$TENANT_ID_ARG}/thresholds"
	const val TENANT_ALERTS = "tenant/{$TENANT_ID_ARG}/alerts"

	fun tenantDash(id: String) = "tenant/$id"
	fun tenantLocations(id: String) = "tenant/$id/locations"
	fun tenantTemplates(id: String) = "tenant/$id/templates"
	fun tenantThresholds(id: String) = "tenant/$id/thresholds"
	fun tenantAlerts(id: String) = "tenant/$id/alerts"

	val tenantIdNavArg = navArgument(TENANT_ID_ARG) { type = NavType.StringType }
}
