package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTemplateRequest(
	val tenantId: String,
	val name: String,
	val scope: TemplateScope,
	val locationId: String? = null,
	val npsEnabled: Boolean = true
)