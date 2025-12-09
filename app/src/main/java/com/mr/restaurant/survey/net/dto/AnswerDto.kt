package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AnswerDto(
	val questionId: String,
	val answer: JsonElement
)
