package com.mr.restaurant.survey.core.net

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
	val error: String? = null,
	val message: String? = null,
	val path: String? = null,
	val status: Int? = null,
	val timestamp: String? = null
)