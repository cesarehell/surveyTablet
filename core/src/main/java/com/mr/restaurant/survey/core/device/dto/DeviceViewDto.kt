package com.mr.restaurant.survey.core.device.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceViewDto(
	val id: String,
	val tenantId: String,
	val tokenSuffix: String? = null,
	val owner: String? = null,
	val label: String? = null,
	val assignedWaiterName: String? = null,
	val defaultTableNo: String? = null,
	val active: Boolean = true,
	val platform: String? = null,
	val deviceType: String? = null,
	val role: String? = null,
	val locationId: String? = null,
	val lastSeen: String? = null,
	val updatedAt: String? = null,
)
