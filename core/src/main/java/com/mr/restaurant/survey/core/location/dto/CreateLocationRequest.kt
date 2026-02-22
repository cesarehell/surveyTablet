package com.mr.restaurant.survey.core.location.dto

@kotlinx.serialization.Serializable
data class CreateLocationRequest(
	val tenantId: String,
	val name: String,
	val city: String? = null,
	val branchName: String? = null,
	val code: String? = null
)

@kotlinx.serialization.Serializable
data class PairingCodeDto(val code: String)

