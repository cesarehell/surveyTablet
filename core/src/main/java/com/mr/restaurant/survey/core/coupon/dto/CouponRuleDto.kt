package com.mr.restaurant.survey.core.coupon.dto

import kotlinx.serialization.Serializable

@Serializable
data class CouponRuleDto(
	val id: String,
	val tenantId: String,
	val templateId: String,
	val locationId: String? = null,
	val questionId: String? = null,
	val active: Boolean = true,
	val type: String,
	val value: Double = 0.0,
	val minVisits: Int? = null,
	val offerType: String? = null,
	val percentOff: Int? = null,
	val amountOff: Double? = null,
	val product: String? = null,
	val campaignId: String? = null,
	val terms: String? = null,
)

@Serializable
data class CouponRuleActiveDto(
	val id: String,
	val active: Boolean,
)
