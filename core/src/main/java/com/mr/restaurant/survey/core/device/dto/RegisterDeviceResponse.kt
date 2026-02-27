package com.mr.restaurant.survey.core.device.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceResponse(
	val status: String? = null,
	val topic: String? = null,
	val pushSubscribed: Boolean? = null,
)
