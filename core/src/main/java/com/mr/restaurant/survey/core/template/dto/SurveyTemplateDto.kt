package com.mr.restaurant.survey.core.template.dto

import com.mr.restaurant.survey.core.location.dto.LocationDto
import kotlinx.serialization.Serializable

@Serializable
data class SurveyTemplateDto(
	val id: String,
	val tenantId: String,
	val groupId: String? = null,
	val name: String,
	val status: TemplateStatus,
	val scope: String? = null,
	val npsEnabled: Boolean = true,
	val location: LocationDto? = null
)