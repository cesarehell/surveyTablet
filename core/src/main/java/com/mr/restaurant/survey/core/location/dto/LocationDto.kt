package com.mr.restaurant.survey.core.location.dto

@kotlinx.serialization.Serializable
data class LocationDto(
	val id: String,          // UUID como string
	val tenantId: String,
	val name: String,
	val city: String? = null,
	val branchName: String? = null,
	val code: String? = null,
	val active: Boolean = true
)
