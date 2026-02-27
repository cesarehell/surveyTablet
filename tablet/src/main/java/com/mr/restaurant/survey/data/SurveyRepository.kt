package com.mr.restaurant.survey.data

import StartSurveyRequestDto
import android.util.Log
import com.mr.restaurant.survey.net.api.SurveyApi
import com.mr.restaurant.survey.net.dto.AlertViewDto
import com.mr.restaurant.survey.net.dto.AnswerDto
import com.mr.restaurant.survey.net.dto.DeviceViewDto
import com.mr.restaurant.survey.net.dto.LocationDto
import com.mr.restaurant.survey.net.dto.PageDto
import com.mr.restaurant.survey.net.dto.PairTabletRequestDto
import com.mr.restaurant.survey.net.dto.PairTabletResponseDto
import com.mr.restaurant.survey.net.dto.QuestionType
import com.mr.restaurant.survey.net.dto.RegisterDeviceRequestDto
import com.mr.restaurant.survey.net.dto.StartSurveyResponseDto
import com.mr.restaurant.survey.net.dto.SubmitResponseDto
import com.mr.restaurant.survey.net.dto.SubmitSurveyRequestDto
import com.mr.restaurant.survey.net.dto.SurveyTemplateDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

class SurveyRepository(
	private val api: SurveyApi
) {
	suspend fun pickCurrentTemplate(tenantId: String, locationId: String? = null): SurveyTemplateDto {
		return if (!locationId.isNullOrBlank()) {
			runCatching { api.getCurrentFull(tenantId, locationId) }
				.getOrElse {
					val page = api.listTemplates(tenantId)
					val templates = page.content
					require(templates.isNotEmpty()) { "No hay templates publicados para $tenantId" }
					templates.firstOrNull { it.location?.id == locationId } ?: templates.first()
				}
		} else {
			val page = api.listTemplates(tenantId)
			val templates = page.content
			require(templates.isNotEmpty()) { "No hay templates publicados para $tenantId" }
			templates.first()
		}
	}

	suspend fun loadTemplateWithOptions(templateId: String): SurveyTemplateDto {
		val t = api.getTemplate(templateId)
		return enrichQuestionOptionsIfMissing(t)
	}

	suspend fun registerDevice(
		tenantId: String,
		token: String,
		owner: String? = null,
		role: String? = null,
		locationId: String? = null,
	) {
		api.registerDevice(
			RegisterDeviceRequestDto(
				tenantId = tenantId,
				token = token,
				owner = owner,
				role = role,
				locationId = locationId
			)
		)
	}

	suspend fun pickFirstPublishedTemplate(tenantId: String): SurveyTemplateDto? =
		api.listTemplates(tenantId).content.firstOrNull()

	suspend fun startSurvey(
		tenant: String,
		templateId: String,
		locationId: String? = null,
		table: String? = null,
		waiter: String? = null
	): StartSurveyResponseDto =
		api.startSurvey(
			StartSurveyRequestDto(
				tenantId = tenant,
				templateId = templateId,
				locationId = locationId,
				tableNo = table,
				waiterName = waiter
			)
		)

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

	suspend fun sendAnswersBatch(instanceId: String, answers: List<AnswerDto>) {
		if (answers.isEmpty()) return
		api.sendAnswers(instanceId, answers)
	}

	suspend fun refreshCurrentTemplate(tenantId: String, locationId: String? = null): SurveyTemplateDto {
		val candidate = if (!locationId.isNullOrBlank()) {
			runCatching { api.getCurrentFull(tenantId, locationId) }
				.getOrElse { pickCurrentTemplate(tenantId, locationId) }
		} else {
			pickCurrentTemplate(tenantId, locationId)
		}
		return enrichQuestionOptionsIfMissing(candidate)
	}

	suspend fun submit(instanceId: String, email: String?, marketingOptIn: Boolean?): SubmitResponseDto {
		return api.submitSurvey(
			instanceId,
			SubmitSurveyRequestDto(email = email, marketingOptIn = marketingOptIn)
		)
	}

	suspend fun getAlerts(tenantId: String): PageDto<AlertViewDto> = api.getAlerts(tenantId)

	suspend fun listLocations(tenantId: String): List<LocationDto> = api.listLocations(tenantId)

	suspend fun pairTablet(pairingCode: String, deviceId: String): PairTabletResponseDto =
		api.pairTablet(PairTabletRequestDto(pairingCode = pairingCode.trim(), deviceId = deviceId))

	suspend fun getTabletAssignment(tenantId: String, owner: String): DeviceViewDto? =
		api.listDevices(tenantId = tenantId, deviceType = "TABLET")
			.firstOrNull { it.owner?.trim() == owner.trim() }

	private suspend fun enrichQuestionOptionsIfMissing(template: SurveyTemplateDto): SurveyTemplateDto {
		val questions = template.questions.orEmpty()
		val needsOptions = questions.any { q ->
			(q.type == QuestionType.SINGLE || q.type == QuestionType.MULTI) &&
				q.options.isNullOrEmpty()
		}
		if (!needsOptions) return template

		val enriched = questions.map { q ->
			if ((q.type == QuestionType.SINGLE || q.type == QuestionType.MULTI) && q.options.isNullOrEmpty()) {
				val opts = api.getQuestionOptions(q.id)
				q.copy(options = opts)
			} else {
				q
			}
		}
		return template.copy(questions = enriched)
	}
}
