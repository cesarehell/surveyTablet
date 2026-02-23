package com.mr.restaurant.survey.core.threshold.dto

import kotlinx.serialization.Serializable

@Serializable
data class ThresholdToggleDto(
	val id: String,
	val active: Boolean
)
