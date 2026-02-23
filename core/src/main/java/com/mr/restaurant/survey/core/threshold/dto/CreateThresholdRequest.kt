package com.mr.restaurant.survey.core.threshold.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateThresholdRequest(
	val tenantId: String,
	val templateId: String,
	val locationId: String? = null,
	val questionId: String? = null,
	val type: String,
	val value: Double,
	val cooldownMin: Int = 10
)
