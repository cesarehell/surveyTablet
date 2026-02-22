package com.mr.restaurant.survey.core.tenant.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TenantStatus {
	ACTIVE,
	INACTIVE
}

@Serializable
data class TenantDto(
	val id: String,
	val name: String,
	val status: TenantStatus
)

@Serializable
data class CreateTenantRequest(
	val name: String,
	val status: TenantStatus? = null
)

@Serializable
data class UpdateTenantRequest(
	val name: String? = null,
	val status: TenantStatus? = null,
	val enableCoupons: Boolean? = null,
	val enableAlerts: Boolean? = null,
	val enablePush: Boolean? = null,
	val maxLocations: Int? = null,
	val maxTabletsPerLocation: Int? = null,
	val maxCouponsPerDay: Int? = null,
	val alertEscalationMin: Int? = null
)
