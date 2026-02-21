package com.mr.restaurant.survey.admin.location

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mr.restaurant.survey.core.location.api.LocationApi
import com.mr.restaurant.survey.core.location.dto.CreateLocationRequest
import com.mr.restaurant.survey.core.location.dto.LocationDto
import com.mr.restaurant.survey.core.location.dto.PairingCodeDto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TenantLocationScreenTest {
	@get:Rule
	val composeRule = createComposeRule()

	@Test
	fun pairingDialog_showsRetryOnlyWhenError() {
		val api = FakeLocationApiAndroid().apply {
			locations += LocationDto(id = "loc-1", tenantId = "tenant-1", name = "Sucursal")
			pairingCodesFailure = IOException("offline")
		}
		val vm = LocationsViewModel(api)

		composeRule.setContent {
			TenantLocationsScreen(
				tenantId = "tenant-1",
				onBack = {},
				vm = vm
			)
		}

		composeRule.onNodeWithText("Codes").performClick()
		composeRule.waitUntil(5_000) {
			composeRule.onAllNodesWithText("Reintentar").fetchSemanticsNodes().isNotEmpty()
		}

		composeRule.onNodeWithText("Cerrar").assertExists()
		composeRule.onNodeWithText("Reintentar").assertExists()
	}

	@Test
	fun pairingDialog_hidesRetryWhenSuccess() {
		val api = FakeLocationApiAndroid().apply {
			locations += LocationDto(id = "loc-1", tenantId = "tenant-1", name = "Sucursal")
			pairingCodesByLocation["loc-1"] = listOf(PairingCodeDto("A-1"))
		}
		val vm = LocationsViewModel(api)

		composeRule.setContent {
			TenantLocationsScreen(
				tenantId = "tenant-1",
				onBack = {},
				vm = vm
			)
		}

		composeRule.onNodeWithText("Codes").performClick()
		composeRule.waitUntil(5_000) {
			composeRule.onAllNodesWithText("Cerrar").fetchSemanticsNodes().isNotEmpty()
		}

		composeRule.onNodeWithText("Cerrar").assertExists()
		composeRule.onNodeWithText("Reintentar").assertDoesNotExist()
	}
}

private class FakeLocationApiAndroid : LocationApi {
	val locations = mutableListOf<LocationDto>()
	val pairingCodesByLocation = mutableMapOf<String, List<PairingCodeDto>>()
	var pairingCodesFailure: Throwable? = null

	override suspend fun list(tenantId: String): List<LocationDto> =
		locations.filter { it.tenantId == tenantId }

	override suspend fun create(req: CreateLocationRequest): LocationDto =
		LocationDto(id = "loc-x", tenantId = req.tenantId, name = req.name)

	override suspend fun pairingCodes(id: String): List<PairingCodeDto> {
		pairingCodesFailure?.let { throw it }
		return pairingCodesByLocation[id].orEmpty()
	}

	override suspend fun setActiveTemplate(id: String, templateId: String): Map<String, String> =
		mapOf("locationId" to id, "templateId" to templateId)

	override suspend fun clearActiveTemplate(id: String): Map<String, String> =
		mapOf("locationId" to id)
}
