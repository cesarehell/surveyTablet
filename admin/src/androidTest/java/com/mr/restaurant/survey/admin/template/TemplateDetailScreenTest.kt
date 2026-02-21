package com.mr.restaurant.survey.admin.template

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateDetailScreenTest {
	@get:Rule
	val composeRule = createComposeRule()

	@Test
	fun addQuestionFailure_keepsInputText() {
		composeRule.setContent {
			TemplateDetailScreen(
				tenantId = "tenant-1",
				loading = false,
				error = null,
				fullName = "Template A",
				status = "DRAFT",
				questions = emptyList(),
				snackbarHostState = remember { SnackbarHostState() },
				resetInputNonce = 0,
				onBack = {},
				onRetry = {},
				onAddQuestion = { _, _, _, _ -> }
			)
		}

		composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Pregunta sin guardar")
		composeRule.onNodeWithText("Agregar").performClick()
		composeRule.onNodeWithText("Pregunta sin guardar").assertExists()
	}

	@Test
	fun addQuestionSuccess_clearsInputText() {
		composeRule.setContent {
			var resetNonce by remember { mutableIntStateOf(0) }
			TemplateDetailScreen(
				tenantId = "tenant-1",
				loading = false,
				error = null,
				fullName = "Template A",
				status = "DRAFT",
				questions = emptyList(),
				snackbarHostState = remember { SnackbarHostState() },
				resetInputNonce = resetNonce,
				onBack = {},
				onRetry = {},
				onAddQuestion = { _, _, _, _ -> resetNonce++ }
			)
		}

		composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Pregunta guardada")
		composeRule.onNodeWithText("Agregar").performClick()
		composeRule.onAllNodesWithText("Pregunta guardada").assertCountEquals(0)
	}

	@Test
	fun errorWithRetry_invokesCallback() {
		var clicked = false
		composeRule.setContent {
			ErrorWithRetry(
				message = "Error",
				onRetry = { clicked = true }
			)
		}

		composeRule.onNodeWithText("Reintentar").assertExists()
		composeRule.onNodeWithText("Reintentar").performClick()
		composeRule.runOnIdle {
			assertTrue(clicked)
		}
	}
}
