package com.mr.restaurant.survey.core.metrics.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetricsDayAggDto(
	val day: String,
	val instances: Long = 0,
	val submitted: Long = 0,
	val alerts: Long = 0
)
