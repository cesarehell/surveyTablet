package com.mr.restaurant.survey.core.metrics.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetricsRuleAggDto(
	val ruleId: String? = null,
	val type: String? = null,
	val threshold: Double? = null,
	val alerts: Long = 0,
	val cooldownMin: Int? = null,
	val active: Boolean? = null,
	val lastTriggeredAt: String? = null,
	val firstSeen: String? = null,
	val lastSeen: String? = null
)
