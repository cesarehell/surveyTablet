package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairTabletRequestDto(
	val pairingCode: String,
	val deviceId: String,
	val platform: String = "ANDROID",
)

@Serializable
data class PairTabletResponseDto(
	val status: String? = null,
	val tenantId: String,
	val locationId: String,
	val locationName: String? = null,
)
