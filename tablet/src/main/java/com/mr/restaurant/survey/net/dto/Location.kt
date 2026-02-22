package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
	val id: String,
	val tenantId: String? = null,
	val name: String? = null,
	val city: String? = null,
	val branchName: String? = null,
	val code: String? = null,
	val active: Boolean? = null
)
