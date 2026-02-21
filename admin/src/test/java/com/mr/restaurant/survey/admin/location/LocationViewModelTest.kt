package com.mr.restaurant.survey.admin.location

import com.mr.restaurant.survey.admin.testutil.FakeLocationApi
import com.mr.restaurant.survey.admin.testutil.MainDispatcherRule
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
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
class LocationViewModelTest {
	@get:Rule
	val mainDispatcherRule = MainDispatcherRule()

	@Test
	fun load_setsErrorWhenApiFails() = runTest {
		val api = FakeLocationApi().apply { listFailure = IOException("offline") }
		val vm = LocationsViewModel(api)

		vm.load("tenant-1")
		advanceUntilIdle()

		assertFalse(vm.state.value.loading)
		assertEquals("Sin conexión o el servidor no responde", vm.state.value.error)
	}

	@Test
	fun create_success_addsLocationAndEmitsCloseDialog() = runTest {
		val api = FakeLocationApi()
		val vm = LocationsViewModel(api)
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.create(
			tenantId = "tenant-1",
			name = "Sucursal Centro",
			city = "Monterrey",
			branchName = "Centro",
			code = "A1"
		)
		advanceUntilIdle()
		job.cancel()

		assertFalse(vm.state.value.loading)
		assertEquals(1, vm.state.value.locations.size)
		assertEquals("Sucursal Centro", vm.state.value.locations.first().name)
		assertTrue(events.any { it is AdminUiEvent.ShowSuccess })
		assertTrue(events.any { it is AdminUiEvent.CloseDialog })
	}

	@Test
	fun create_error_emitsErrorAndDoesNotCloseDialog() = runTest {
		val api = FakeLocationApi().apply { createFailure = IOException("offline") }
		val vm = LocationsViewModel(api)
		val events = mutableListOf<AdminUiEvent>()
		val job = launch { vm.events.collect { events += it } }

		vm.create(
			tenantId = "tenant-1",
			name = "Sucursal Norte",
			city = null,
			branchName = null,
			code = null
		)
		advanceUntilIdle()
		job.cancel()

		assertFalse(vm.state.value.loading)
		assertTrue(vm.state.value.locations.isEmpty())
		assertTrue(events.any { it is AdminUiEvent.ShowError })
		assertTrue(events.none { it is AdminUiEvent.CloseDialog })
	}

	@Test
	fun loadPairingCodes_updatesStateOnSuccessAndError() = runTest {
		val api = FakeLocationApi().apply {
			locations += LocationDto(id = "loc-1", tenantId = "tenant-1", name = "Sucursal")
			pairingCodesByLocation["loc-1"] = listOf(PairingCodeDto("ABC123"))
		}
		val vm = LocationsViewModel(api)

		vm.loadPairingCodes("loc-1")
		advanceUntilIdle()

		assertFalse(vm.state.value.pairingCodesLoading)
		assertEquals(1, vm.state.value.pairingCodes.size)
		assertEquals("ABC123", vm.state.value.pairingCodes.first().code)

		api.pairingCodesFailure = IOException("offline")
		vm.loadPairingCodes("loc-1")
		advanceUntilIdle()

		assertFalse(vm.state.value.pairingCodesLoading)
		assertEquals("Sin conexión o el servidor no responde", vm.state.value.pairingCodesError)
	}
}
