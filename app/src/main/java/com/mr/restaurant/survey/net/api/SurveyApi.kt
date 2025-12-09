package com.mr.restaurant.survey.net.api

import StartSurveyRequestDto
import com.mr.restaurant.survey.net.dto.AlertViewDto
import com.mr.restaurant.survey.net.dto.AnswerDto
import com.mr.restaurant.survey.net.dto.PageDto
import com.mr.restaurant.survey.net.dto.QOptionDTO
import com.mr.restaurant.survey.net.dto.RegisterDeviceRequestDto
import com.mr.restaurant.survey.net.dto.StartSurveyResponseDto
import com.mr.restaurant.survey.net.dto.SubmitResponseDto
import com.mr.restaurant.survey.net.dto.SurveyTemplateDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SurveyApi {
	@GET(ApiRoutes.CURRENT_TEMPLATE)
	suspend fun getCurrent(@Query(ApiQuery.TENANT_ID) tenantId: String): SurveyTemplateDto

	@POST(ApiRoutes.REGISTER_DEVICE)
	suspend fun registerDevice(@Body req: RegisterDeviceRequestDto)

	@GET(ApiRoutes.CURRENT_TEMPLATE_FULL)
	suspend fun getCurrentFull(@Query(ApiQuery.TENANT_ID) tenantId: String): SurveyTemplateDto

	@GET(ApiRoutes.TEMPLATE_BY_ID)
	suspend fun getTemplate(@Path("id") id: String): SurveyTemplateDto

	@GET(ApiRoutes.TEMPLATES)
	suspend fun listTemplates(
		@Query(ApiQuery.TENANT_ID) tenantId: String,
		@Query(ApiQuery.STATUS) status: String = ApiDefaults.TEMPLATE_STATUS_PUBLISHED
	): PageDto<SurveyTemplateDto>

	@GET(ApiRoutes.QUESTIONS_OPTIONS)
	suspend fun getQuestionOptions(@Path("id") questionId: String): List<QOptionDTO>

	@POST(ApiRoutes.START_SURVEY)
	suspend fun startSurvey(@Body req: StartSurveyRequestDto): StartSurveyResponseDto

	@POST(ApiRoutes.SURVEY_ANSWERS)
	suspend fun sendAnswers(
		@Path("instId") instanceId: String, @Body answers: List<AnswerDto>
	)

	@POST(ApiRoutes.SUBMIT_SURVEY)
	suspend fun submitSurvey(@Path("instId") instanceId: String): SubmitResponseDto

	@GET(ApiRoutes.ALERTS)
	suspend fun getAlerts(@Query(ApiQuery.TENANT_ID) tenantId: String): PageDto<AlertViewDto>
}