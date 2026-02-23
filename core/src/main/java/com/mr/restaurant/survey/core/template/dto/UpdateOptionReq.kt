package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateOptionReq(
	val label: String? = null,
	val value: String? = null,
	val oOrder: Int? = null,
)
