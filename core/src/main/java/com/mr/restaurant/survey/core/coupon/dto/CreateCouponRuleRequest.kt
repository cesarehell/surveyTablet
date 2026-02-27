package com.mr.restaurant.survey.core.coupon.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateCouponRuleRequest(
	val tenantId: String,
	val templateId: String,
	val locationId: String? = null,
	val questionId: String? = null,
	val type: String,
	val value: Double,
	val active: Boolean = true,
	val minVisits: Int? = null,
	val offerType: String = "PERCENT",
	val percentOff: Int? = null,
	val amountOff: Double? = null,
	val product: String? = null,
	val campaignId: String? = null,
	val terms: String? = null,
)
