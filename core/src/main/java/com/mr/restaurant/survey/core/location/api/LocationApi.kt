package com.mr.restaurant.survey.core.location.api

import com.mr.restaurant.survey.core.location.dto.CreateLocationRequest
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LocationApi {

	@GET("/v1/locations")
	suspend fun list(@Query("tenantId") tenantId: String): List<LocationDto>

	@POST("/v1/locations")
	suspend fun create(@Body req: CreateLocationRequest): LocationDto

	@GET("/v1/locations/{id}/pairing-codes")
	suspend fun pairingCodes(@Path("id") id: String): List<PairingCodeDto>

	@POST("/v1/locations/{id}/active-template")
	suspend fun setActiveTemplate(@Path("id") id: String, @Query("templateId") templateId: String): Map<String, String>

	@DELETE("/v1/locations/{id}/active-template")
	suspend fun clearActiveTemplate(@Path("id") id: String): Map<String, String>
}
