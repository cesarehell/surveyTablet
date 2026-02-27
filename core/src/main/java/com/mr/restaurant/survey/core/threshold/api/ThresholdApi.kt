package com.mr.restaurant.survey.core.threshold.api

import com.mr.restaurant.survey.core.threshold.dto.CreateThresholdRequest
import com.mr.restaurant.survey.core.threshold.dto.ThresholdRuleDto
import com.mr.restaurant.survey.core.threshold.dto.ThresholdToggleDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ThresholdApi {
	@GET("/v1/thresholds")
	suspend fun list(@Query("templateId") templateId: String): List<ThresholdRuleDto>

	@POST("/v1/thresholds")
	suspend fun create(@Body req: CreateThresholdRequest): ThresholdRuleDto

	@PATCH("/v1/thresholds/{id}/active")
	suspend fun setActive(
		@Path("id") id: String,
		@Query("active") active: Boolean
	): ThresholdToggleDto

	@DELETE("/v1/thresholds/{id}")
	suspend fun delete(@Path("id") id: String): Map<String, String>
}
