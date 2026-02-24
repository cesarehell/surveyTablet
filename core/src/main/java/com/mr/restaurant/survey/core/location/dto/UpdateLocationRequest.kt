package com.mr.restaurant.survey.core.location.dto

@kotlinx.serialization.Serializable
data class UpdateLocationRequest(
	val name: String? = null,
	val city: String? = null,
	val branchName: String? = null,
	val code: String? = null,
	val active: Boolean? = null,
)
