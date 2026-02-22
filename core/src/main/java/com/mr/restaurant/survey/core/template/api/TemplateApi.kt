package com.mr.restaurant.survey.core.template.api

import com.mr.restaurant.survey.core.template.dto.AddQuestionRequest
import com.mr.restaurant.survey.core.template.dto.CreateOptionReq
import com.mr.restaurant.survey.core.template.dto.CreateTemplateRequest
import com.mr.restaurant.survey.core.template.dto.PageDto
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import com.mr.restaurant.survey.core.template.dto.TemplateStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TemplateApi {

	@GET("/v1/templates")
	suspend fun listPaged(
		@Query("tenantId") tenantId: String,
		@Query("status") status: String? = null,
		@Query("page") page: Int = 0,
		@Query("size") size: Int = 200
	): PageDto<SurveyTemplateDto>

	@POST("/v1/templates")
	suspend fun create(@Body req: CreateTemplateRequest): SurveyTemplateDto

	@POST("/v1/templates/{id}/publish")
	suspend fun publish(@Path("id") id: String): SurveyTemplateDto

	@PATCH("/v1/templates/{id}/status")
	suspend fun setStatus(
		@Path("id") id: String,
		@Query("status") status: TemplateStatus
	): SurveyTemplateDto

	@GET("/v1/templates/{id}/full")
	suspend fun getFull(@Path("id") id: String): TemplateFullDto

	@POST("/v1/templates/{id}/questions")
	suspend fun addQuestion(
		@Path("id") id: String,
		@Body req: AddQuestionRequest
	): Map<String, String>

	@POST("/v1/questions/{id}/options")
	suspend fun addOptions(
		@Path("id") questionId: String,
		@Body opts: List<CreateOptionReq>
	): List<Map<String, String>>
}

