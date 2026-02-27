package com.mr.restaurant.survey.core.threshold.dto

import com.mr.restaurant.survey.core.location.dto.LocationDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThresholdRuleDto(
	val id: String,
	val tenantId: String? = null,
	@JsonNames("questionId", "question_id")
	val questionId: String? = null,
	@JsonNames("questionText", "question_text")
	val questionText: String? = null,
	val question: ThresholdRuleQuestionRefDto? = null,
	val active: Boolean = true,
	val type: String? = null,
	val value: Double = 0.0,
	val cooldownMin: Int = 0,
	val lastTriggeredAt: String? = null,
	val location: LocationDto? = null,
	val createdAt: String? = null,
	val updatedAt: String? = null
) {
	val resolvedQuestionId: String?
		get() = when {
			!questionId.isNullOrBlank() -> questionId
			!question?.id.isNullOrBlank() -> question.id
			!question?.questionId.isNullOrBlank() -> question.questionId
			else -> null
		}

	val resolvedQuestionLabel: String?
		get() = when {
			!questionText.isNullOrBlank() -> questionText
			!question?.text.isNullOrBlank() -> question.text
			else -> null
		}
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class ThresholdRuleQuestionRefDto(
	@JsonNames("id", "questionId", "question_id")
	val id: String? = null,
	@JsonNames("questionId", "question_id")
	val questionId: String? = null,
	@JsonNames("text", "questionText", "question_text")
	val text: String? = null
)
