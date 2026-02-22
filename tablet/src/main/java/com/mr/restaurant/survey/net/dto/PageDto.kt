package com.mr.restaurant.survey.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageDto<T>(
	val content: List<T> = emptyList(),
	val totalElements: Int = 0,
	val totalPages: Int = 0,
	val size: Int = 0,
	val number: Int = 0,
	val first: Boolean = true,
	val last: Boolean = true,
	val numberOfElements: Int = 0,
	val empty: Boolean = true
)