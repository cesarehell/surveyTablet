package com.mr.restaurant.survey.core.tenant

import com.mr.restaurant.survey.core.tenant.api.TenantApi
import com.mr.restaurant.survey.core.tenant.dto.CreateTenantRequest
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus
import com.mr.restaurant.survey.core.tenant.dto.UpdateTenantRequest

class TenantRepository(private val api: TenantApi) {
	suspend fun list(): List<TenantDto> = api.listTenants()
	suspend fun create(name: String): TenantDto = api.createTenant(CreateTenantRequest(name.trim()))
	suspend fun setStatus(id: String, status: TenantStatus): TenantDto =
		api.updateTenant(id, UpdateTenantRequest(status = status))
}
