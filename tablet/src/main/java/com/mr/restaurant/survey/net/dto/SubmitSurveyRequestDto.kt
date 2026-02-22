package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubmitSurveyRequestDto(
	val email: String? = null,
	val marketingOptIn: Boolean? = null
)
