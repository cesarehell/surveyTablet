package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubmitResponseDto(
	val status: String,
	val alerts: List<AlertViewDto> = emptyList(),
	val coupon: CouponViewDto? = null,
	val visitCount: Long? = null,
	val couponStatus: String? = null,
)
