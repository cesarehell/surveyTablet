package com.mr.restaurant.survey.core.device.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateDeviceAssignmentRequest(
	val label: String? = null,
	val assignedWaiterName: String? = null,
	val defaultTableNo: String? = null,
	val active: Boolean? = null,
)
