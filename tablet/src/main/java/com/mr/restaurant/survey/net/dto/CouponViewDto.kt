package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class CouponViewDto(
	val code: String? = null,
	val qr: String? = null,
	val status: String? = null,
	val expiresAt: String? = null,
	val offerType: String? = null,
	val percentOff: Int? = null,
	val amountOff: String? = null,
	val product: String? = null,
	val campaignId: String? = null,
	val terms: String? = null,
)
