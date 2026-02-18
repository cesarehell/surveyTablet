package com.mr.restaurant.survey.core.tenant.api

import com.mr.restaurant.survey.core.tenant.dto.CreateTenantRequest
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.UpdateTenantRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TenantApi {

	@GET("/v1/tenants")
	suspend fun listTenants(): List<TenantDto>

	@POST("/v1/tenants")
	suspend fun createTenant(@Body req: CreateTenantRequest): TenantDto

	@PATCH("/v1/tenants/{id}")
	suspend fun updateTenant(
		@Path("id") id: String,
		@Body req: UpdateTenantRequest
	): TenantDto
}
