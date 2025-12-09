package com.mr.restaurant.survey.data

import StartSurveyRequestDto
import android.util.Log
import com.mr.restaurant.survey.net.api.SurveyApi
import com.mr.restaurant.survey.net.dto.AlertViewDto
import com.mr.restaurant.survey.net.dto.AnswerDto
import com.mr.restaurant.survey.net.dto.PageDto
import com.mr.restaurant.survey.net.dto.QuestionType
import com.mr.restaurant.survey.net.dto.RegisterDeviceRequestDto
import com.mr.restaurant.survey.net.dto.StartSurveyResponseDto
import com.mr.restaurant.survey.net.dto.SubmitResponseDto
import com.mr.restaurant.survey.net.dto.SurveyTemplateDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

class SurveyRepository(
	private val api: SurveyApi
) {
	suspend fun pickCurrentTemplate(tenantId: String): SurveyTemplateDto {

		Log.d("tenantId", "id = $tenantId")
		return runCatching {
			api.getCurrentFull(tenantId)
		}
			.getOrElse {
				val page = api.listTemplates(tenantId)
				val templates = page.content
				require(templates.isNotEmpty()) { "No hay templates publicados para $tenantId" }
				templates.first()
			}
	}

	suspend fun loadTemplateWithOptions(templateId: String): SurveyTemplateDto {
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
		api.registerDevice(RegisterDeviceRequestDto(tenantId, token, owner))
	}

	suspend fun pickFirstPublishedTemplate(tenantId: String): SurveyTemplateDto? =
		api.listTemplates(tenantId).content.firstOrNull()

	suspend fun startSurvey(
		tenant: String,
		templateId: String,
		locationId: String? = null,
		table: String? = null,
		waiter: String? = null
	): StartSurveyResponseDto {
		return api.startSurvey(
			StartSurveyRequestDto(
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
		api.sendAnswers(instanceId, listOf(AnswerDto(questionId = questionId, answer = json)))
	}

	suspend fun submit(instanceId: String): SubmitResponseDto = api.submitSurvey(instanceId)

	suspend fun getAlerts(tenantId: String): PageDto<AlertViewDto> = api.getAlerts(tenantId)
}
