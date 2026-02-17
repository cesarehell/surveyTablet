package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceResponseDto(
	val status: String,
	val topic: String? = null
)
