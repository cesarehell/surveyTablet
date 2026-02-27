package com.mr.restaurant.survey.core.metrics.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetricsSummaryDto(
	val tenantId: String,
	val from: String,
	val to: String,
	val locationId: String? = null,
	val instances: Long = 0,
	val submitted: Long = 0,
	val alertsInWindow: Long = 0,
	val openAlertsNow: Long = 0,
	val ackAlertsNow: Long = 0,
	val conversionRate: Double = 0.0
)
