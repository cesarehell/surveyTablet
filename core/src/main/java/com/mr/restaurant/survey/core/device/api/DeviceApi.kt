package com.mr.restaurant.survey.core.device.api

import com.mr.restaurant.survey.core.device.dto.RegisterDeviceRequest
import com.mr.restaurant.survey.core.device.dto.RegisterDeviceResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApi {
	@POST("/v1/devices/register")
	suspend fun register(@Body req: RegisterDeviceRequest): RegisterDeviceResponse
}
