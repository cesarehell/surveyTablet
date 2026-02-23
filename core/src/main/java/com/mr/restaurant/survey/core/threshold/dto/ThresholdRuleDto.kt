package com.mr.restaurant.survey.core.threshold.dto

import com.mr.restaurant.survey.core.location.dto.LocationDto
import kotlinx.serialization.Serializable

@Serializable
data class ThresholdRuleDto(
	val id: String,
	val tenantId: String? = null,
	val active: Boolean = true,
	val type: String? = null,
	val value: Double = 0.0,
	val cooldownMin: Int = 0,
	val lastTriggeredAt: String? = null,
	val location: LocationDto? = null,
	val createdAt: String? = null,
	val updatedAt: String? = null
)
