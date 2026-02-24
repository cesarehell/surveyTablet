package com.mr.restaurant.survey.admin.navigation

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

object Routes {
	const val TENANTS = "tenants"

	const val TENANT_ID_ARG = "tenantId"
	const val TEMPLATE_ID_ARG = "templateId"

	const val TENANT_DASH = "tenant/{$TENANT_ID_ARG}"
	const val TENANT_LOCATIONS = "tenant/{$TENANT_ID_ARG}/locations"
	const val TENANT_DEVICES = "tenant/{$TENANT_ID_ARG}/devices"
	const val TENANT_TEMPLATES = "tenant/{$TENANT_ID_ARG}/templates"
	const val TENANT_METRICS = "tenant/{$TENANT_ID_ARG}/metrics"
	const val TENANT_THRESHOLDS = "tenant/{$TENANT_ID_ARG}/thresholds"
	const val TENANT_ALERTS = "tenant/{$TENANT_ID_ARG}/alerts"
	const val TENANT_PUSH_DEBUG = "tenant/{$TENANT_ID_ARG}/push-debug"
	const val CREATE_TEMPLATE = "createTemplate/{$TENANT_ID_ARG}"
	const val TEMPLATE_DETAIL = "templateDetail/{$TENANT_ID_ARG}/{$TEMPLATE_ID_ARG}"

	private fun encode(value: String): String = Uri.encode(value)

	fun tenantDash(id: String) = "tenant/${encode(id)}"
	fun tenantLocations(id: String) = "tenant/${encode(id)}/locations"
	fun tenantDevices(id: String) = "tenant/${encode(id)}/devices"
	fun tenantTemplates(id: String) = "tenant/${encode(id)}/templates"
	fun tenantMetrics(id: String) = "tenant/${encode(id)}/metrics"
	fun tenantThresholds(id: String) = "tenant/${encode(id)}/thresholds"
	fun tenantAlerts(id: String) = "tenant/${encode(id)}/alerts"
	fun tenantPushDebug(id: String) = "tenant/${encode(id)}/push-debug"
	fun createTemplate(tenantId: String) = "createTemplate/${encode(tenantId)}"
	fun templateDetail(tenantId: String, templateId: String) =
		"templateDetail/${encode(tenantId)}/${encode(templateId)}"

	val tenantIdNavArg = navArgument(TENANT_ID_ARG) { type = NavType.StringType }
	val templateIdNavArg = navArgument(TEMPLATE_ID_ARG) { type = NavType.StringType }
}
