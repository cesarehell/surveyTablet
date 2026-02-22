package com.mr.restaurant.survey.admin.tenant

import com.mr.restaurant.survey.admin.testutil.FakeTenantApi
import com.mr.restaurant.survey.admin.testutil.MainDispatcherRule
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.tenant.TenantRepository
import com.mr.restaurant.survey.core.tenant.dto.TenantDto
import com.mr.restaurant.survey.core.tenant.dto.TenantStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TenantViewModelTest {
	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun load_setsErrorWhenApiFails() = runTest {
		val api = FakeTenantApi().apply {
			listFailure = IOException("offline")
		}
		val vm = TenantViewModel(TenantRepository(api))

		vm.load()
		advanceUntilIdle()

		assertFalse(vm.state.value.loading)
		assertEquals("Sin conexión o el servidor no responde", vm.state.value.error)
	}

	@Test
	fun create_refreshesListAfterSuccess() = runTest {
		val api = FakeTenantApi()
		val vm = TenantViewModel(TenantRepository(api))

		vm.create("MR Fish")
		advanceUntilIdle()

		assertFalse(vm.state.value.loading)
		assertNull(vm.state.value.error)
		assertEquals(1, vm.state.value.tenants.size)
		assertEquals("MR Fish", vm.state.value.tenants.first().name)
	}

	@Test
	fun create_mapsConflictToFriendlyMessage() = runTest {
		val api = FakeTenantApi().apply {
			createFailure = HttpException(
				Response.error<TenantDto>(
					409,
					"""{"message":"exists"}""".toResponseBody("application/json".toMediaType())
				)
			)
		}
		val vm = TenantViewModel(TenantRepository(api))
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.create("Duplicado")
		advanceUntilIdle()
		job.cancel()

		assertFalse(vm.state.value.loading)
		assertNull(vm.state.value.error)
		val error = events.filterIsInstance<AdminUiEvent.ShowError>().firstOrNull()
		assertEquals("Ya existe ese tenant.", error?.message)
	}

	@Test
	fun toggle_updatesTenantStatusAndRefreshes() = runTest {
		val api = FakeTenantApi().apply {
			tenants += TenantDto(id = "t-1", name = "MR", status = TenantStatus.ACTIVE)
		}
		val vm = TenantViewModel(TenantRepository(api))
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.toggle(api.tenants.first())
		advanceUntilIdle()
		job.cancel()

		assertFalse(vm.state.value.loading)
		assertEquals(TenantStatus.INACTIVE, vm.state.value.tenants.first().status)
		assertTrue(events.any { it is AdminUiEvent.ShowSuccess })
	}
}
