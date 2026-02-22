package com.mr.restaurant.survey.core.template.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageDto<T>(
	val content: List<T> = emptyList(),
	val number: Int? = null,
	val totalElements: Long? = null,
	val totalPages: Int? = null
)
