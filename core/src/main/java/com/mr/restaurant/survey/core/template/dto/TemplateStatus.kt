package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TemplateStatus {
	DRAFT,
	PUBLISHED,
	ARCHIVED
}