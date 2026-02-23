package com.mr.restaurant.survey.core.metrics.api

import com.mr.restaurant.survey.core.metrics.dto.MetricsDayAggDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsItemCountDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsRuleAggDto
import com.mr.restaurant.survey.core.metrics.dto.MetricsSummaryDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MetricsApi {
	@GET("/v1/admin/metrics/summary")
	suspend fun summary(@Query("tenantId") tenantId: String): MetricsSummaryDto

	@GET("/v1/admin/metrics/series/daily")
	suspend fun daily(@Query("tenantId") tenantId: String): List<MetricsDayAggDto>

	@GET("/v1/admin/metrics/top/tables")
	suspend fun topTables(
		@Query("tenantId") tenantId: String,
		@Query("limit") limit: Int = 10
	): List<MetricsItemCountDto>

	@GET("/v1/admin/metrics/top/waiters")
	suspend fun topWaiters(
		@Query("tenantId") tenantId: String,
		@Query("limit") limit: Int = 10
	): List<MetricsItemCountDto>

	@GET("/v1/admin/metrics/rules")
	suspend fun rules(@Query("tenantId") tenantId: String): List<MetricsRuleAggDto>
}
