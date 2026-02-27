package com.mr.restaurant.survey.core.coupon.api

import com.mr.restaurant.survey.core.coupon.dto.CouponRuleActiveDto
import com.mr.restaurant.survey.core.coupon.dto.CouponRuleDto
import com.mr.restaurant.survey.core.coupon.dto.CreateCouponRuleRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CouponRuleApi {
	@POST("/v1/coupon-rules")
	suspend fun create(@Body req: CreateCouponRuleRequest): CouponRuleDto

	@GET("/v1/coupon-rules")
	suspend fun list(@Query("templateId") templateId: String): List<CouponRuleDto>

	@PATCH("/v1/coupon-rules/{id}/active")
	suspend fun setActive(@Path("id") id: String, @Query("active") active: Boolean): CouponRuleActiveDto

	@DELETE("/v1/coupon-rules/{id}")
	suspend fun delete(@Path("id") id: String): Map<String, String>
}
