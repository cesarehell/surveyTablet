package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceViewDto(
	val id: String,
	val tenantId: String,
	val owner: String? = null,
	val label: String? = null,
	val assignedWaiterName: String? = null,
	val defaultTableNo: String? = null,
	val active: Boolean = true,
	val deviceType: String? = null,
	val role: String? = null,
	val locationId: String? = null,
)
