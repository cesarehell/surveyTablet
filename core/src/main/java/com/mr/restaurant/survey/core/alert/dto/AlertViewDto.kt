package com.mr.restaurant.survey.core.alert.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlertViewDto(
	val id: String,
	val tenantId: String,
	val severity: String? = null,
	val state: String? = null,
	val reason: String? = null,
	val escalated: Boolean = false,
	val escalatedAt: String? = null,
	val instanceId: String? = null,
	val tableNo: String? = null,
	val waiterName: String? = null,
	val startedAt: String? = null,
	val ruleId: String? = null,
	val ruleType: String? = null,
	val ruleValue: Double? = null,
	val questionId: String? = null
)
