package com.mr.restaurant.survey.admin.template

import com.mr.restaurant.survey.admin.testutil.FakeTemplateApi
import com.mr.restaurant.survey.admin.testutil.MainDispatcherRule
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.template.dto.SurveyTemplateDto
import com.mr.restaurant.survey.core.template.dto.TemplateScope
import com.mr.restaurant.survey.core.template.dto.TemplateStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateViewModelTest {
	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun load_setsErrorWhenApiFails() = runTest {
		val api = FakeTemplateApi().apply {
			listFailure = IOException("offline")
		}
		val vm = TemplateViewModel(api)

		vm.load("tenant-1")
		advanceUntilIdle()

		assertFalse(vm.state.value.loading)
		assertEquals("Sin conexión o el servidor no responde", vm.state.value.error)
	}

	@Test
	fun create_emitsNavigationEventAndRefreshesTemplates() = runTest {
		val api = FakeTemplateApi()
		val vm = TemplateViewModel(api)
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.create(
			tenantId = "tenant-1",
			name = "Servicio",
			scope = TemplateScope.TENANT,
			locationId = null,
			npsEnabled = true
		)
		advanceUntilIdle()
		job.cancel()

		assertFalse(vm.state.value.loading)
		assertTrue(vm.state.value.templates.any { it.name == "Servicio" })
		assertTrue(events.any { it is AdminUiEvent.ShowSuccess })
		assertTrue(events.any { it is AdminUiEvent.NavigateToTemplateDetail })
	}

	@Test
	fun setFilter_keepsOnlyRequestedStatus() = runTest {
		val api = FakeTemplateApi().apply {
			templates += SurveyTemplateDto(
				id = "tpl-1",
				tenantId = "tenant-1",
				name = "A",
				status = TemplateStatus.DRAFT,
				scope = TemplateScope.TENANT.name
			)
			templates += SurveyTemplateDto(
				id = "tpl-2",
				tenantId = "tenant-1",
				name = "B",
				status = TemplateStatus.PUBLISHED,
				scope = TemplateScope.LOCATION.name,
				location = LocationDto("loc-1", "tenant-1", "Sucursal")
			)
		}
		val vm = TemplateViewModel(api)

		vm.setFilter(TemplateStatus.PUBLISHED, "tenant-1")
		advanceUntilIdle()

		assertEquals(1, vm.state.value.templates.size)
		assertEquals(TemplateStatus.PUBLISHED, vm.state.value.templates.first().status)
	}

	@Test
	fun togglePublish_archivedTemplate_setsFriendlyError() = runTest {
		val api = FakeTemplateApi()
		val vm = TemplateViewModel(api)
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }
		val archived = SurveyTemplateDto(
			id = "tpl-archived",
			tenantId = "tenant-1",
			name = "Archivado",
			status = TemplateStatus.ARCHIVED,
			scope = TemplateScope.TENANT.name
		)

		vm.togglePublish("tenant-1", archived)
		advanceUntilIdle()
		job.cancel()

		val error = events.filterIsInstance<AdminUiEvent.ShowError>().firstOrNull()
		assertEquals("No se puede publicar un template archivado", error?.message)
		assertFalse(vm.state.value.loading)
	}
}
