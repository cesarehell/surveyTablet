package com.mr.restaurant.survey.admin.testutil

import com.mr.restaurant.survey.core.location.api.LocationApi
import com.mr.restaurant.survey.core.location.dto.CreateLocationRequest
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.template.dto.AddQuestionRequest
import com.mr.restaurant.survey.core.template.dto.CreateOptionReq
import com.mr.restaurant.survey.core.template.dto.CreateTemplateRequest
import com.mr.restaurant.survey.core.template.dto.PageDto
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import com.mr.restaurant.survey.core.template.dto.TemplateStatus
import com.mr.restaurant.survey.core.tenant.api.TenantApi
import com.mr.restaurant.survey.core.tenant.dto.CreateTenantRequest
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus
import com.mr.restaurant.survey.core.tenant.dto.UpdateTenantRequest

class FakeLocationApi : LocationApi {
	val locations = mutableListOf<LocationDto>()
	val pairingCodesByLocation = mutableMapOf<String, List<PairingCodeDto>>()
	var listFailure: Throwable? = null
	var createFailure: Throwable? = null
	var pairingCodesFailure: Throwable? = null

	override suspend fun list(tenantId: String): List<LocationDto> {
		listFailure?.let { throw it }
		return locations.filter { it.tenantId == tenantId }
	}

	override suspend fun create(req: CreateLocationRequest): LocationDto {
		createFailure?.let { throw it }
		val location = LocationDto(
			id = "loc-${locations.size + 1}",
			tenantId = req.tenantId,
			name = req.name,
			city = req.city,
			branchName = req.branchName,
			code = req.code
		)
		locations += location
		return location
	}

	override suspend fun pairingCodes(id: String): List<PairingCodeDto> {
		pairingCodesFailure?.let { throw it }
		return pairingCodesByLocation[id].orEmpty()
	}

	override suspend fun setActiveTemplate(id: String, templateId: String): Map<String, String> {
		return mapOf("locationId" to id, "templateId" to templateId)
	}

	override suspend fun clearActiveTemplate(id: String): Map<String, String> {
		return mapOf("locationId" to id, "templateId" to "")
	}
}

class FakeTenantApi : TenantApi {
	val tenants = mutableListOf<TenantDto>()

	var listFailure: Throwable? = null
	var createFailure: Throwable? = null
	var updateFailure: Throwable? = null

	override suspend fun listTenants(): List<TenantDto> {
		listFailure?.let { throw it }
		return tenants.toList()
	}

	override suspend fun createTenant(req: CreateTenantRequest): TenantDto {
		createFailure?.let { throw it }
		val created = TenantDto(
			id = "t-${tenants.size + 1}",
			name = req.name,
			status = req.status ?: TenantStatus.ACTIVE
		)
		tenants += created
		return created
	}

	override suspend fun updateTenant(id: String, req: UpdateTenantRequest): TenantDto {
		updateFailure?.let { throw it }
		val idx = tenants.indexOfFirst { it.id == id }
		check(idx >= 0) { "Tenant not found: $id" }

		val current = tenants[idx]
		val updated = current.copy(
			name = req.name ?: current.name,
			status = req.status ?: current.status
		)
		tenants[idx] = updated
		return updated
	}
}

class FakeTemplateApi : TemplateApi {
	val templates = mutableListOf<SurveyTemplateDto>()
	private val fullById = mutableMapOf<String, TemplateFullDto>()
	private var nextTemplateId = 1
	private var nextQuestionId = 1

	var listFailure: Throwable? = null
	var createFailure: Throwable? = null
	var publishFailure: Throwable? = null
	var statusFailure: Throwable? = null
	var getFullFailure: Throwable? = null
	var addQuestionFailure: Throwable? = null

	override suspend fun listPaged(
		tenantId: String,
		status: String?,
		page: Int,
		size: Int
	): PageDto<SurveyTemplateDto> {
		listFailure?.let { throw it }

		val filtered = templates
			.filter { it.tenantId == tenantId }
			.filter { status == null || it.status.name == status }

		return PageDto(content = filtered)
	}

	override suspend fun create(req: CreateTemplateRequest): SurveyTemplateDto {
		createFailure?.let { throw it }
		val id = "tpl-${nextTemplateId++}"
		val created = SurveyTemplateDto(
			id = id,
			tenantId = req.tenantId,
			groupId = null,
			name = req.name,
			status = TemplateStatus.DRAFT,
			scope = req.scope.name,
			npsEnabled = req.npsEnabled,
			location = req.locationId?.let { LocationDto(id = it, tenantId = req.tenantId, name = "Loc $it") }
		)
		templates += created
		fullById[id] = TemplateFullDto(
			id = id,
			tenantId = req.tenantId,
			name = req.name,
			status = TemplateStatus.DRAFT.name,
			npsEnabled = req.npsEnabled,
			scope = req.scope.name,
			locationId = req.locationId,
			questions = emptyList()
		)
		return created
	}

	override suspend fun publish(id: String): SurveyTemplateDto {
		publishFailure?.let { throw it }
		return updateStatus(id, TemplateStatus.PUBLISHED)
	}

	override suspend fun setStatus(id: String, status: TemplateStatus): SurveyTemplateDto {
		statusFailure?.let { throw it }
		return updateStatus(id, status)
	}

	override suspend fun getFull(id: String): TemplateFullDto {
		getFullFailure?.let { throw it }
		return fullById[id] ?: error("Template full not found: $id")
	}

	override suspend fun addQuestion(
		id: String,
		req: AddQuestionRequest
	): Map<String, String> {
		addQuestionFailure?.let { throw it }

		val full = fullById[id] ?: error("Template full not found: $id")
		val qId = "q-${nextQuestionId++}"
		val question = TemplateFullDto.QuestionDto(
			id = qId,
			order = req.order,
			text = req.text,
			type = req.type,
			required = req.required,
			options = emptyList()
		)
		fullById[id] = full.copy(questions = full.questions + question)
		return mapOf("id" to qId)
	}

	override suspend fun addOptions(
		questionId: String,
		opts: List<CreateOptionReq>
	): List<Map<String, String>> = opts.mapIndexed { idx, _ ->
		mapOf("id" to "$questionId-opt-${idx + 1}")
	}

	private fun updateStatus(id: String, status: TemplateStatus): SurveyTemplateDto {
		val idx = templates.indexOfFirst { it.id == id }
		check(idx >= 0) { "Template not found: $id" }

		val updated = templates[idx].copy(status = status)
		templates[idx] = updated

		val full = fullById[id]
		if (full != null) {
			fullById[id] = full.copy(status = status.name)
		}

		return updated
	}
}
