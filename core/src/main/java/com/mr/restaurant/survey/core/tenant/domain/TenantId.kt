package com.mr.restaurant.survey.core.tenant.domain

@JvmInline
value class TenantId(val value: String) {
	override fun toString(): String = value
}
