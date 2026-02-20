package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateOptionReq(
	val label: String,
	val value: String,
	val oOrder: Int? = null
)
