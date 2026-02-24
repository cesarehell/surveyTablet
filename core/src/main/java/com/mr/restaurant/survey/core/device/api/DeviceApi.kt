package com.mr.restaurant.survey.core.device.api

import com.mr.restaurant.survey.core.device.dto.RegisterDeviceRequest
import com.mr.restaurant.survey.core.device.dto.RegisterDeviceResponse
import com.mr.restaurant.survey.core.device.dto.DeviceViewDto
import com.mr.restaurant.survey.core.device.dto.UpdateDeviceAssignmentRequest
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DeviceApi {
	@POST("/v1/devices/register")
	suspend fun register(@Body req: RegisterDeviceRequest): RegisterDeviceResponse

	@GET("/v1/devices")
	suspend fun list(
		@Query("tenantId") tenantId: String,
		@Query("deviceType") deviceType: String = "TABLET",
	): List<DeviceViewDto>

	@PATCH("/v1/devices/{id}")
	suspend fun update(
		@Path("id") id: String,
		@Query("tenantId") tenantId: String,
		@Body req: UpdateDeviceAssignmentRequest
	): DeviceViewDto
}
