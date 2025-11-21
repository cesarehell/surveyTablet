package com.mr.restaurant.survey.net

import retrofit2.http.*

interface SurveyApi {
      @GET("v1/templates/current")
     suspend fun getCurrent(@Query("tenantId") tenantId: String): SurveyTemplateDTO

    @POST("v1/devices/register")
    suspend fun registerDevice(@Body req: RegisterDeviceReq)

     @GET("v1/templates/current/full")
      suspend fun getCurrentFull(@Query("tenantId") tenantId: String): SurveyTemplateDTO

    @GET("v1/templates/{id}")
    suspend fun getTemplate(@Path("id") id: String): SurveyTemplateDTO

    @GET("v1/templates")
    suspend fun listTemplates(
        @Query("tenantId") tenantId: String,
        @Query("status") status: String = "PUBLISHED"
    ): List<SurveyTemplateDTO>

    @GET("v1/questions/{id}/options")
    suspend fun getQuestionOptions(@Path("id") questionId: String): List<QOptionDTO>

    @POST("v1/surveys/start")
    suspend fun startSurvey(@Body req: StartSurveyReq): StartSurveyResp

    @POST("v1/surveys/{instId}/answers")
    suspend fun sendAnswers(
        @Path("instId") instanceId: String,
        @Body answers: List<AnswerDTO>
    )

    @POST("v1/surveys/{instId}/submit")
    suspend fun submitSurvey(@Path("instId") instanceId: String): SubmitResp

    @GET("v1/alerts")
    suspend fun getAlerts(@Query("tenantId") tenantId: String): List<AlertView>
}
