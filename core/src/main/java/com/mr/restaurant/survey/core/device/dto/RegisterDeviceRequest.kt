package com.mr.restaurant.survey.core.device.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
	val tenantId: String,
	val token: String,
	val owner: String? = null,
	val platform: String = "ANDROID",
	val role: String? = null,
	val deviceType: String? = null,
	val locationId: String? = null,
)
