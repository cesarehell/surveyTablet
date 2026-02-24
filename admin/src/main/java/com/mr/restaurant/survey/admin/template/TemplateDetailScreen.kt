package com.mr.restaurant.survey.admin.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto

@Composable
fun TemplateDetailRoute(
	tenantId: String,
	templateId: String,
	onBack: () -> Unit,
	vm: TemplateDetailViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }
	var resetInputNonce by remember { mutableIntStateOf(0) }

	LaunchedEffect(templateId) { vm.load(templateId) }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
				is AdminUiEvent.ShowSuccess -> {
					resetInputNonce++
					snackbarHostState.showSnackbar(event.message)
				}

				is AdminUiEvent.NavigateToTemplateDetail -> Unit
				AdminUiEvent.CloseDialog -> Unit
			}
		}
	}

	TemplateDetailScreen(
		tenantId = tenantId,
		loading = st.loading,
		error = st.error,
		fullName = st.full?.name,
		status = st.full?.status,
		questions = st.full?.questions.orEmpty(),
		onBack = onBack,
		onRetry = { vm.load(templateId) },
		snackbarHostState = snackbarHostState,
		resetInputNonce = resetInputNonce,
		onAddQuestion = { order, type, text, required ->
			vm.addQuestion(templateId, order, type, text, required)
		},
		onMoveQuestion = { questionId, direction ->
			vm.moveQuestion(templateId, questionId, direction)
		},
		onUpdateQuestion = { questionId, order, type, text, required ->
			vm.updateQuestion(templateId, questionId, order, type, text, required)
		},
		onDeleteQuestion = { questionId ->
			vm.deleteQuestion(templateId, questionId)
		},
		onAddOption = { questionId, label, value ->
			vm.addOption(templateId, questionId, label, value)
		},
		onUpdateOption = { questionId, optionId, label, value, order ->
			vm.updateOption(templateId, questionId, optionId, label, value, order)
		},
		onDeleteOption = { questionId, optionId ->
			vm.deleteOption(templateId, questionId, optionId)
		},
	)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TemplateDetailScreen(
	tenantId: String,
	loading: Boolean,
	error: String?,
	fullName: String?,
	status: String?,
	questions: List<TemplateFullDto.QuestionDto>,
	snackbarHostState: SnackbarHostState,
	resetInputNonce: Int,
	onBack: () -> Unit,
	onRetry: () -> Unit,
	onAddQuestion: (order: Int, type: String, text: String, required: Boolean) -> Unit,
	onMoveQuestion: (questionId: String, direction: Int) -> Unit,
	onUpdateQuestion: (questionId: String, order: Int, type: String, text: String, required: Boolean) -> Unit,
	onDeleteQuestion: (questionId: String) -> Unit,
	onAddOption: (questionId: String, label: String, value: String) -> Unit,
	onUpdateOption: (questionId: String, optionId: String, label: String, value: String, order: Int) -> Unit,
	onDeleteOption: (questionId: String, optionId: String) -> Unit
) {
	var qText by rememberSaveable { mutableStateOf("") }
	var qType by rememberSaveable { mutableStateOf("LIKERT_5") }
	var required by rememberSaveable { mutableStateOf(true) }
	var editQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
	var deleteQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
	var optionsEditorQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
	val sortedQuestions = remember(questions) { questions.sortedBy { it.order } }
	val optionsEditorQuestion = questions.firstOrNull { it.id == optionsEditorQuestionId }
	val editQuestion = questions.firstOrNull { it.id == editQuestionId }
	val deleteQuestion = questions.firstOrNull { it.id == deleteQuestionId }
	LaunchedEffect(resetInputNonce) {
		if (resetInputNonce > 0) {
			qText = ""
			qType = "LIKERT_5"
			required = true
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
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
				ErrorWithRetry(
					message = error,
					onRetry = onRetry
				)
			}

			if (loading) {
				LinearProgressIndicator(Modifier.fillMaxWidth())
			}

			Card(Modifier.fillMaxWidth()) {
				Column(
					modifier = Modifier.padding(12.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					Text("Agregar pregunta", style = MaterialTheme.typography.titleMedium)
					OutlinedTextField(
						value = qText,
						onValueChange = { qText = it },
						label = { Text("Texto de la pregunta") },
						modifier = Modifier.fillMaxWidth()
					)
					Text(
						"Tipo de respuesta",
						style = MaterialTheme.typography.labelMedium
					)
					QuestionTypeSelector(
						selectedType = qType,
						onSelect = { qType = it }
					)
					Row(
						Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Column {
							Text("Obligatoria", style = MaterialTheme.typography.bodyMedium)
							Text(
								if (required) "Sí, requiere respuesta" else "No, opcional",
								style = MaterialTheme.typography.bodySmall
							)
						}
						Switch(checked = required, onCheckedChange = { required = it })
					}
				}
			}

				Button(
					onClick = {
						val order = (sortedQuestions.maxOfOrNull { it.order } ?: 0) + 1
						onAddQuestion(order, qType, qText.trim(), required)
					},
					enabled = qText.isNotBlank() && !loading,
					modifier = Modifier.fillMaxWidth()
				) {
					Text("Agregar")
				}

			Spacer(Modifier.height(8.dp))
			Text("Preguntas (${questions.size})", style = MaterialTheme.typography.titleMedium)

			if (!loading && questions.isEmpty()) {
				EmptyState(
					message = "Este template todavía no tiene preguntas.",
					modifier = Modifier.weight(1f)
				)
			} else {
				LazyColumn(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					itemsIndexed(sortedQuestions, key = { _, item -> item.id }) { index, q ->
						Card(Modifier.fillMaxWidth()) {
							Column(
								Modifier.padding(12.dp),
								verticalArrangement = Arrangement.spacedBy(6.dp)
							) {
								Text(
									"#${q.order} · ${questionTypeLabel(q.type)}",
									style = MaterialTheme.typography.titleSmall
								)
								Spacer(Modifier.height(4.dp))
								Text(
									q.text,
									style = MaterialTheme.typography.bodyLarge
								)
								Text(
									if (q.required) "Obligatoria" else "Opcional",
									style = MaterialTheme.typography.bodySmall
								)
								FlowRow(
									horizontalArrangement = Arrangement.spacedBy(8.dp),
									verticalArrangement = Arrangement.spacedBy(8.dp)
								) {
									Button(
										onClick = { onMoveQuestion(q.id, -1) },
										enabled = !loading && index > 0
									) { Text("↑") }
									Button(
										onClick = { onMoveQuestion(q.id, +1) },
										enabled = !loading && index < sortedQuestions.lastIndex
									) { Text("↓") }
									Button(
										onClick = { editQuestionId = q.id },
										enabled = !loading
									) { Text("Editar") }
									TextButton(
										onClick = { deleteQuestionId = q.id },
										enabled = !loading
									) { Text("Borrar") }
								}
								if (q.type == "SINGLE" || q.type == "MULTI") {
									Text(
										"Opciones (${q.options.size})",
										style = MaterialTheme.typography.bodySmall
									)
									q.options.sortedBy { it.oOrder }.forEach { opt ->
										Text(
											"• ${opt.label} · ${opt.value}",
											style = MaterialTheme.typography.bodySmall,
											maxLines = 1,
											overflow = TextOverflow.Ellipsis
										)
									}
									Button(
										onClick = { optionsEditorQuestionId = q.id },
										enabled = !loading,
										modifier = Modifier.fillMaxWidth()
									) { Text("Gestionar opciones") }
								}
							}
						}
					}
				}
			}
		}
	}

	optionsEditorQuestion?.let { question ->
		QuestionOptionsDialog(
			question = question,
			loading = loading,
			onDismiss = { optionsEditorQuestionId = null },
			onAddOption = { label, value -> onAddOption(question.id, label, value) },
			onUpdateOption = { optionId, label, value, order ->
				onUpdateOption(question.id, optionId, label, value, order)
			},
			onDeleteOption = { optionId -> onDeleteOption(question.id, optionId) }
		)
	}

	editQuestion?.let { question ->
		EditQuestionDialog(
			question = question,
			loading = loading,
			onDismiss = { editQuestionId = null },
			onSave = { order, type, text, isRequired ->
				onUpdateQuestion(question.id, order, type, text, isRequired)
				editQuestionId = null
			}
		)
	}

	deleteQuestion?.let { question ->
		AlertDialog(
			onDismissRequest = { deleteQuestionId = null },
			title = { Text("Eliminar pregunta") },
			text = { Text("¿Eliminar \"${question.text}\"? Esta acción no se puede deshacer.") },
			confirmButton = {
				Button(
					onClick = {
						onDeleteQuestion(question.id)
						deleteQuestionId = null
					},
					enabled = !loading
				) { Text("Eliminar") }
			},
			dismissButton = {
				TextButton(onClick = { deleteQuestionId = null }) { Text("Cancelar") }
			}
		)
	}
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EditQuestionDialog(
	question: TemplateFullDto.QuestionDto,
	loading: Boolean,
	onDismiss: () -> Unit,
	onSave: (order: Int, type: String, text: String, required: Boolean) -> Unit
) {
	var text by remember(question.id, question.text) { mutableStateOf(question.text) }
	var type by remember(question.id, question.type) { mutableStateOf(question.type) }
	var required by remember(question.id, question.required) { mutableStateOf(question.required) }
	var orderText by remember(question.id, question.order) { mutableStateOf(question.order.toString()) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Editar pregunta") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				OutlinedTextField(
					value = text,
					onValueChange = { text = it },
					label = { Text("Texto") },
					modifier = Modifier.fillMaxWidth()
				)
				OutlinedTextField(
					value = orderText,
					onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
					label = { Text("Orden") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				QuestionTypeSelector(
					selectedType = type,
					onSelect = { type = it }
				)
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
					Text("Obligatoria")
					Switch(checked = required, onCheckedChange = { required = it })
				}
			}
		},
		confirmButton = {
			Button(
				onClick = {
					onSave(orderText.toIntOrNull() ?: question.order, type, text, required)
				},
				enabled = !loading && text.isNotBlank()
			) { Text("Guardar") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("Cancelar") }
		}
	)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionTypeSelector(
	selectedType: String,
	onSelect: (String) -> Unit
) {
	FlowRow(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		listOf("LIKERT_5", "YESNO", "TEXT", "SINGLE", "MULTI").forEach { type ->
			FilterChip(
				selected = selectedType == type,
				onClick = { onSelect(type) },
				label = { Text(questionTypeLabel(type)) }
			)
		}
	}
}

private fun questionTypeLabel(type: String): String = when (type.uppercase()) {
	"LIKERT_5" -> "Likert 1-5"
	"YESNO" -> "Sí / No"
	"TEXT" -> "Texto"
	"SINGLE" -> "Selección única"
	"MULTI" -> "Selección múltiple"
	else -> type
}

@Composable
private fun QuestionOptionsDialog(
	question: TemplateFullDto.QuestionDto,
	loading: Boolean,
	onDismiss: () -> Unit,
	onAddOption: (label: String, value: String) -> Unit,
	onUpdateOption: (optionId: String, label: String, value: String, order: Int) -> Unit,
	onDeleteOption: (optionId: String) -> Unit
) {
	var newLabel by remember(question.id) { mutableStateOf("") }
	var newValue by remember(question.id) { mutableStateOf("") }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Opciones: ${question.text}") },
		text = {
			Column(
				verticalArrangement = Arrangement.spacedBy(10.dp),
				modifier = Modifier.verticalScroll(rememberScrollState())
			) {
				if (question.options.isEmpty()) {
					Text(
						"Sin opciones. Agrega al menos una para que la tablet pueda mostrar esta pregunta.",
						style = MaterialTheme.typography.bodySmall
					)
				}

				question.options.sortedBy { it.oOrder }.forEach { option ->
					EditableOptionRow(
						option = option,
						enabled = !loading,
						onSave = { label, value, order ->
							onUpdateOption(option.id, label, value, order)
						},
						onDelete = { onDeleteOption(option.id) }
					)
				}

				Spacer(Modifier.height(4.dp))
				Text("Agregar opción", style = MaterialTheme.typography.titleSmall)
				OutlinedTextField(
					value = newLabel,
					onValueChange = { newLabel = it },
					label = { Text("Label") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				OutlinedTextField(
					value = newValue,
					onValueChange = { newValue = it },
					label = { Text("Value") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				Button(
					onClick = {
						onAddOption(newLabel, newValue)
						newLabel = ""
						newValue = ""
					},
					enabled = !loading && newLabel.isNotBlank() && newValue.isNotBlank(),
					modifier = Modifier.fillMaxWidth()
				) { Text("Agregar opción") }
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("Cerrar") }
		}
	)
}

@Composable
private fun EditableOptionRow(
	option: TemplateFullDto.OptionDto,
	enabled: Boolean,
	onSave: (label: String, value: String, order: Int) -> Unit,
	onDelete: () -> Unit
) {
	var label by remember(option.id, option.label) { mutableStateOf(option.label) }
	var value by remember(option.id, option.value) { mutableStateOf(option.value) }
	var orderText by remember(option.id, option.oOrder) { mutableStateOf(option.oOrder.toString()) }

	Card(Modifier.fillMaxWidth()) {
		Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			OutlinedTextField(
				value = label,
				onValueChange = { label = it },
				label = { Text("Label") },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				enabled = enabled
			)
			OutlinedTextField(
				value = value,
				onValueChange = { value = it },
				label = { Text("Value") },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				enabled = enabled
			)
			OutlinedTextField(
				value = orderText,
				onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
				label = { Text("Orden") },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				enabled = enabled
			)
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Button(
					onClick = { onSave(label, value, orderText.toIntOrNull() ?: 0) },
					enabled = enabled && label.isNotBlank() && value.isNotBlank()
				) { Text("Guardar") }
				TextButton(onClick = onDelete, enabled = enabled) { Text("Eliminar") }
			}
		}
	}
}
