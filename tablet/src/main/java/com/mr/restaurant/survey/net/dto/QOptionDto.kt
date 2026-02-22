package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class QOptionDTO(
	val id: String,
	val label: String,
	val value: String,
	val oOrder: Int = 0
)
