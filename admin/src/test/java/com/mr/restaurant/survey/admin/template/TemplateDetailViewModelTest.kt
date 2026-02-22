package com.mr.restaurant.survey.admin.template

import com.mr.restaurant.survey.admin.testutil.FakeTemplateApi
import com.mr.restaurant.survey.admin.testutil.MainDispatcherRule
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.template.dto.CreateTemplateRequest
import com.mr.restaurant.survey.core.template.dto.TemplateScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateDetailViewModelTest {
	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun addQuestion_withBlankText_setsValidationError() = runTest {
		val api = FakeTemplateApi()
		val vm = TemplateDetailViewModel(api)
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.addQuestion(
			templateId = "tpl-1",
			order = 1,
			type = "TEXT",
			text = "   ",
			required = true
		)
		advanceUntilIdle()
		job.cancel()

		val error = events.filterIsInstance<AdminUiEvent.ShowError>().firstOrNull()
		assertEquals("La pregunta no puede ir vacía", error?.message)
		assertFalse(vm.state.value.loading)
	}

	@Test
	fun addQuestion_success_refreshesTemplateFull() = runTest {
		val api = FakeTemplateApi()
		val created = api.create(
			CreateTemplateRequest(
				tenantId = "tenant-1",
				name = "Template A",
				scope = TemplateScope.TENANT
			)
		)
		val vm = TemplateDetailViewModel(api)
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.load(created.id)
		advanceUntilIdle()
		assertTrue(vm.state.value.full?.questions?.isEmpty() == true)

		vm.addQuestion(
			templateId = created.id,
			order = 1,
			type = "TEXT",
			text = "Como te fue?",
			required = true
		)
		advanceUntilIdle()
		job.cancel()

		assertFalse(vm.state.value.loading)
		assertEquals(1, vm.state.value.full?.questions?.size)
		assertEquals("Como te fue?", vm.state.value.full?.questions?.first()?.text)
		assertTrue(events.any { it is AdminUiEvent.ShowSuccess })
	}
}
