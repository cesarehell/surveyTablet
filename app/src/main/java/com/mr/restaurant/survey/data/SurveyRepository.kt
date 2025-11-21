package com.mr.restaurant.survey.data

import com.mr.restaurant.survey.net.*
import kotlinx.serialization.json.*

class SurveyRepository(
    private val api: SurveyApi
) {
       suspend fun pickCurrentTemplate(tenantId: String): SurveyTemplateDTO {
              return runCatching { api.getCurrentFull(tenantId) }.getOrElse {
                       val list = api.listTemplates(tenantId)
                       require(list.isNotEmpty()) { "No hay templates publicados para $tenantId" }
                      list.first()
                   }
            }

    suspend fun loadTemplateWithOptions(templateId: String): SurveyTemplateDTO {
        val t = api.getTemplate(templateId)
        val enriched = t.questions?.map { q ->
            if (q.type == QuestionType.SINGLE || q.type == QuestionType.MULTI) {
                val opts = api.getQuestionOptions(q.id)
                q.copy(options = opts)
            } else q
        }
        return t.copy(questions = enriched)
    }

    suspend fun registerDevice(tenantId: String, token: String, owner: String? = null) {
        api.registerDevice(RegisterDeviceReq(tenantId, token, owner))
    }

    suspend fun pickFirstPublishedTemplate(tenantId: String): SurveyTemplateDTO? {
        val list = api.listTemplates(tenantId)
        return list.firstOrNull()
    }

    suspend fun startSurvey(
        tenant: String,
        templateId: String,
        locationId: String? = null,
        table: String? = null,
        waiter: String? = null
    ): StartSurveyResp {
        return api.startSurvey(
            StartSurveyReq(
                tenantId = tenant,
                templateId = templateId,
                locationId = locationId,
                tableNo = table,
                waiterName = waiter
            )
        )
    }

    suspend fun postAnswer(instanceId: String, questionId: String, value: Any) {
        val json: JsonElement = when (value) {
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is List<*> -> JsonArray(value.filterNotNull().map { JsonPrimitive(it.toString()) })
            else -> JsonPrimitive(value.toString())
        }
        api.sendAnswers(instanceId, listOf(AnswerDTO(questionId = questionId, answer = json)))
    }

    suspend fun submit(instanceId: String): SubmitResp = api.submitSurvey(instanceId)

    suspend fun getAlerts(tenantId: String): List<AlertView> = api.getAlerts(tenantId)
}
