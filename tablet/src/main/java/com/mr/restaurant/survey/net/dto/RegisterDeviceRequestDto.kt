package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequestDto(
	val tenantId: String,
	val token: String,
	val owner: String? = null,
	val platform: String = "ANDROID",
	val deviceType: String = "TABLET",
	val role: String? = null,
	val locationId: String? = null,
)
