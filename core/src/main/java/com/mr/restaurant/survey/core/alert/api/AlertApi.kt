package com.mr.restaurant.survey.core.alert.api

import com.mr.restaurant.survey.core.alert.dto.AlertViewDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AlertApi {
	@GET("/v1/alerts")
	suspend fun list(@Query("tenantId") tenantId: String): List<AlertViewDto>

	@POST("/v1/alerts/{id}/ack")
	suspend fun ack(@Path("id") id: String): Map<String, String>
}
