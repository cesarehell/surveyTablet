package com.mr.restaurant.survey.admin.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto

@Composable
fun TemplateDetailRoute(
	tenantId: String,
	templateId: String,
	onBack: () -> Unit,
	vm: TemplateDetailViewModel = viewModel()
) {
	val st by vm.state.collectAsState()

	LaunchedEffect(templateId) { vm.load(templateId) }

	TemplateDetailScreen(
		tenantId = tenantId,
		templateId = templateId,
		loading = st.loading,
		error = st.error,
		fullName = st.full?.name,
		status = st.full?.status?.toString(),
		questions = st.full?.questions.orEmpty(),
		onBack = onBack,
		onAddQuestion = { order, type, text, required ->
			vm.addQuestion(templateId, order, type, text, required)
		}
	)
}

@Composable
fun TemplateDetailScreen(
	tenantId: String,
	templateId: String,
	loading: Boolean,
	error: String?,
	fullName: String?,
	status: String?,
	questions: List<TemplateFullDto.QuestionDto>,
	onBack: () -> Unit,
	onAddQuestion: (order: Int, type: String, text: String, required: Boolean) -> Unit
) {
	var qText by rememberSaveable { mutableStateOf("") }
	var qType by rememberSaveable { mutableStateOf("RATING") }
	var required by rememberSaveable { mutableStateOf(true) }

	Scaffold(
		topBar = {
			SimpleTopBar(
				title = fullName ?: "Template",
				subtitle = "Tenant: $tenantId · Status: ${status ?: "-"}",
				onBack = onBack
			)
		}
	) { padding ->

		Column(
			Modifier
				.padding(padding)
				.padding(16.dp)
				.fillMaxSize(),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {

			if (error != null) {
				ErrorBanner(message = error)
			}

			if (loading) {
				LinearProgressIndicator(Modifier.fillMaxWidth())
			}

			Text("Agregar pregunta", style = MaterialTheme.typography.titleMedium)

			OutlinedTextField(
				value = qText,
				onValueChange = { qText = it },
				label = { Text("Texto") },
				modifier = Modifier.fillMaxWidth()
			)

			Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
				Button(onClick = { qType = "RATING" }, enabled = qType != "RATING") { Text("RATING") }
				Button(onClick = { qType = "TEXT" }, enabled = qType != "TEXT") { Text("TEXT") }
				Button(onClick = { qType = "SINGLE_CHOICE" }, enabled = qType != "SINGLE_CHOICE") { Text("CHOICE") }
			}

			Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
				Text("Required")
				Switch(checked = required, onCheckedChange = { required = it })
			}

			Button(
				onClick = {
					val order = questions.size + 1
					onAddQuestion(order, qType, qText.trim(), required)
					qText = ""
					qType = "RATING"
					required = true
				},
				enabled = qText.isNotBlank() && !loading,
				modifier = Modifier.fillMaxWidth()
			) {
				Text("Agregar")
			}

			Spacer(Modifier.height(8.dp))
			Text("Preguntas (${questions.size})", style = MaterialTheme.typography.titleMedium)

			LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				items(questions, key = { it.id }) { q ->
					Card(Modifier.fillMaxWidth()) {
						Column(Modifier.padding(12.dp)) {
							Text("#${q.order} · ${q.type}", style = MaterialTheme.typography.titleSmall)
							Spacer(Modifier.height(4.dp))
							Text(q.text)
							Spacer(Modifier.height(4.dp))
							Text("required=${q.required}", style = MaterialTheme.typography.bodySmall)
						}
					}
				}
			}
		}
	}
}