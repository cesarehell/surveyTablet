package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
enum class QuestionType {
	LIKERT_5,
	LIKERT_10,
	YES_NO,
	SINGLE,
	MULTI,
	TEXT
}