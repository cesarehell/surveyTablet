package com.mr.restaurant.survey.core.metrics.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetricsItemCountDto(
	val key: String,
	val count: Long = 0
)
