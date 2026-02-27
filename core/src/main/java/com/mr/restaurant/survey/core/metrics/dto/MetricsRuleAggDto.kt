package com.mr.restaurant.survey.core.metrics.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MetricsRuleAggDto(
	val ruleId: String? = null,
	val type: String? = null,
	val threshold: Double? = null,
	val alerts: Long = 0,
	val cooldownMin: Int? = null,
	val active: Boolean? = null,
	@JsonNames("questionId", "question_id")
	val questionId: String? = null,
	@JsonNames("questionText", "question_text")
	val questionText: String? = null,
	val lastTriggeredAt: String? = null,
	val firstSeen: String? = null,
	val lastSeen: String? = null
)
