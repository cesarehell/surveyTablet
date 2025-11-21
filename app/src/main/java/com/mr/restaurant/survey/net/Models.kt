package com.mr.restaurant.survey.net

import kotlinx.serialization.Serializable

@Serializable
data class SurveyTemplateDTO(
    val id: String,
    val tenantId: String,
    val name: String,
    val status: String? = null,
    val npsEnabled: Boolean = true,
    val questions: List<QuestionDTO>? = null
)

@Serializable
data class QuestionDTO(
    val id: String,
    val tenantId: String? = null,
    val type: QuestionType,
    val text: String,
    val required: Boolean = true,
    val qorder: Int? = null,
    val options: List<QOptionDTO>? = null
)

@Serializable
enum class QuestionType { LIKERT_5, LIKERT_10, YESNO, SINGLE, MULTI, TEXT }

@Serializable
data class StartSurveyReq(
    val tenantId: String,
    val templateId: String,
    val locationId: String? = null,
    val tableNo: String? = null,
    val waiterName: String? = null
)

@Serializable data class StartSurveyResp(val instanceId: String, val templateId: String? = null)

@Serializable data class AnswerDTO(val questionId: String, val answer: kotlinx.serialization.json.JsonElement)

@Serializable
data class AlertView(
    val id: String,
    val tenantId: String,
    val severity: String? = null,
    val state: String? = null,
    val reason: String? = null,
    val instanceId: String? = null,
    val tableNo: String? = null,
    val waiterName: String? = null,
    val startedAt: String? = null,
    val ruleId: String? = null,
    val ruleType: String? = null,
    val ruleValue: Double? = null,
    val questionId: String? = null
)

@Serializable data class SubmitResp(val status: String, val alerts: List<AlertView> = emptyList())

@Serializable
data class QOptionDTO(
    val id: String,
    val label: String,
    val value: String,
    val oOrder: Int = 0
)

@Serializable
data class RegisterDeviceReq(
    val tenantId: String,
    val token: String,
    val owner: String? = null,
    val platform: String = "ANDROID"
)

@Serializable
data class RegisterDeviceResp(
    val status: String,
    val topic: String? = null
)
