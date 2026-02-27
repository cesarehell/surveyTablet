package com.mr.restaurant.survey.admin.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.R
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.DialogCancelButton
import com.mr.restaurant.survey.admin.ui.DialogConfirmButton
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.FilterChipWrap
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.admin.ui.adminQuestionTypeLabel
import com.mr.restaurant.survey.core.template.dto.TemplateFullDto
import kotlinx.coroutines.launch

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
				is AdminUiEvent.ShowError -> launch { snackbarHostState.showSnackbar(event.message) }
				is AdminUiEvent.ShowSuccess -> {
					if (event.message.startsWith("Pregunta ")) {
						resetInputNonce++
					}
					launch { snackbarHostState.showSnackbar(event.message) }
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
	var editorMode by rememberSaveable { mutableStateOf("CREATE") }
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
			editorMode = "MANAGE"
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			SimpleTopBar(
				title = fullName ?: stringResource(R.string.template_detail_fallback_title),
				subtitle = stringResource(R.string.template_detail_subtitle, tenantId, status ?: "-"),
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

			FilterChipWrap(
				items = listOf("CREATE", "MANAGE"),
				selectedItem = editorMode,
				onSelect = { editorMode = it },
				labelContent = {
					Text(
						when (it) {
							"CREATE" -> stringResource(R.string.template_detail_mode_create)
							else -> stringResource(R.string.template_detail_mode_manage)
						}
					)
				}
			)

			Text(
				stringResource(R.string.template_detail_questions_count, questions.size),
				style = MaterialTheme.typography.titleMedium
			)

			if (editorMode == "CREATE") {
				Card(Modifier.fillMaxWidth()) {
					Column(
						modifier = Modifier.padding(12.dp),
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {
						Text(
							stringResource(R.string.template_detail_add_question_title),
							style = MaterialTheme.typography.titleMedium
						)
						OutlinedTextField(
							value = qText,
							onValueChange = { qText = it },
							label = { Text(stringResource(R.string.template_detail_question_text)) },
							modifier = Modifier.fillMaxWidth()
						)
						Text(
							stringResource(R.string.template_detail_answer_type),
							style = MaterialTheme.typography.labelMedium
						)
						QuestionTypeSelector(
							selectedType = qType,
							onSelect = { qType = it }
						)
						Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
							Column {
								Text(
									stringResource(R.string.template_detail_required),
									style = MaterialTheme.typography.bodyMedium
								)
								Text(
									if (required) stringResource(R.string.template_detail_required_yes)
									else stringResource(R.string.template_detail_required_no),
									style = MaterialTheme.typography.bodySmall
								)
							}
							Switch(checked = required, onCheckedChange = { required = it })
						}
						Button(
							onClick = {
								val order = (sortedQuestions.maxOfOrNull { it.order } ?: 0) + 1
								onAddQuestion(order, qType, qText.trim(), required)
							},
							enabled = qText.isNotBlank() && !loading,
							modifier = Modifier.fillMaxWidth()
						) {
							Text(stringResource(R.string.template_detail_create_question_action))
						}
					}
				}
				if (questions.isNotEmpty()) {
					Text(
						stringResource(R.string.template_detail_mode_hint_manage),
						style = MaterialTheme.typography.bodySmall
					)
				}
			} else {
				if (!loading && questions.isEmpty()) {
					EmptyState(
						message = stringResource(R.string.template_detail_empty_questions),
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
									verticalArrangement = Arrangement.spacedBy(8.dp)
								) {
									Text(
										"#${index + 1} · ${adminQuestionTypeLabel(q.type)}",
										style = MaterialTheme.typography.titleSmall
									)
									Text(
										q.text,
										style = MaterialTheme.typography.bodyLarge,
										maxLines = 3,
										overflow = TextOverflow.Ellipsis
									)
									Text(
										if (q.required) stringResource(R.string.template_detail_required_badge)
										else stringResource(R.string.template_detail_optional_badge),
										style = MaterialTheme.typography.bodySmall
									)
									QuestionActionRow(
										loading = loading,
										canMoveUp = index > 0,
										canMoveDown = index < sortedQuestions.lastIndex,
										onMoveUp = { onMoveQuestion(q.id, -1) },
										onMoveDown = { onMoveQuestion(q.id, +1) },
										onEdit = { editQuestionId = q.id },
										onDelete = { deleteQuestionId = q.id }
									)
									if (q.type == "SINGLE" || q.type == "MULTI") {
										Text(
											stringResource(R.string.template_detail_options_count, q.options.size),
											style = MaterialTheme.typography.bodySmall
										)
										q.options.sortedBy { it.oOrder }.take(2).forEach { opt ->
											Text(
												"• ${opt.label} · ${opt.value}",
												style = MaterialTheme.typography.bodySmall,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis
											)
										}
										if (q.options.size > 2) {
											Text(
												stringResource(
													R.string.template_detail_options_more_count,
													q.options.size - 2
												),
												style = MaterialTheme.typography.bodySmall
											)
										}
										OutlinedButton(
											onClick = { optionsEditorQuestionId = q.id },
											enabled = !loading,
											modifier = Modifier.fillMaxWidth()
										) { Text(stringResource(R.string.template_detail_manage_options)) }
									}
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
			onSave = { type, text, isRequired ->
				onUpdateQuestion(question.id, question.order, type, text, isRequired)
				editQuestionId = null
			}
		)
	}

	deleteQuestion?.let { question ->
		AlertDialog(
			onDismissRequest = { deleteQuestionId = null },
			title = { Text(stringResource(R.string.template_detail_delete_question_title)) },
			text = { Text(stringResource(R.string.template_detail_delete_question_message, question.text)) },
			confirmButton = {
				DialogConfirmButton(
					text = stringResource(R.string.common_delete),
					enabled = !loading,
					onClick = {
						onDeleteQuestion(question.id)
						deleteQuestionId = null
					}
				)
			},
			dismissButton = {
				DialogCancelButton(onClick = { deleteQuestionId = null })
			}
		)
	}
}

@Composable
private fun QuestionActionRow(
	loading: Boolean,
	canMoveUp: Boolean,
	canMoveDown: Boolean,
	onMoveUp: () -> Unit,
	onMoveDown: () -> Unit,
	onEdit: () -> Unit,
	onDelete: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			CompactMoveButton(
				label = "↑",
				enabled = !loading && canMoveUp,
				onClick = onMoveUp
			)
			CompactMoveButton(
				label = "↓",
				enabled = !loading && canMoveDown,
				onClick = onMoveDown
			)
		}
		Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
			TextButton(
				onClick = onEdit,
				enabled = !loading
			) { Text(stringResource(R.string.common_edit)) }
			TextButton(
				onClick = onDelete,
				enabled = !loading
			) { Text(stringResource(R.string.common_delete)) }
		}
	}
}

@Composable
private fun CompactMoveButton(
	label: String,
	enabled: Boolean,
	onClick: () -> Unit
) {
	OutlinedButton(
		onClick = onClick,
		enabled = enabled,
		modifier = Modifier.width(52.dp)
	) {
		Text(label)
	}
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EditQuestionDialog(
	question: TemplateFullDto.QuestionDto,
	loading: Boolean,
	onDismiss: () -> Unit,
	onSave: (type: String, text: String, required: Boolean) -> Unit
) {
	var text by remember(question.id, question.text) { mutableStateOf(question.text) }
	var type by remember(question.id, question.type) { mutableStateOf(question.type) }
	var required by remember(question.id, question.required) { mutableStateOf(question.required) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.template_detail_edit_question_title)) },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
				OutlinedTextField(
					value = text,
					onValueChange = { text = it },
					label = { Text(stringResource(R.string.template_name)) },
					modifier = Modifier.fillMaxWidth()
				)
				Text(
					stringResource(R.string.template_detail_order_note, question.order),
					style = MaterialTheme.typography.bodySmall
				)
				QuestionTypeSelector(
					selectedType = type,
					onSelect = { type = it }
				)
				Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
					Text(stringResource(R.string.template_detail_required))
					Switch(checked = required, onCheckedChange = { required = it })
				}
			}
		},
		confirmButton = {
			DialogConfirmButton(
				text = stringResource(R.string.common_save),
				enabled = !loading && text.isNotBlank(),
				onClick = { onSave(type, text, required) }
			)
		},
		dismissButton = {
			DialogCancelButton(onClick = onDismiss)
		}
	)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionTypeSelector(
	selectedType: String,
	onSelect: (String) -> Unit
) {
	FilterChipWrap(
		items = listOf("LIKERT_5", "YESNO", "TEXT", "SINGLE", "MULTI"),
		selectedItem = selectedType,
		onSelect = onSelect,
		labelContent = { Text(adminQuestionTypeLabel(it)) }
	)
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
	var lastKnownOptionsCount by remember(question.id) { mutableStateOf(question.options.size) }
	var showHelp by remember(question.id) { mutableStateOf(false) }

	LaunchedEffect(question.options.size) {
		if (question.options.size > lastKnownOptionsCount) {
			newLabel = ""
			newValue = ""
		}
		lastKnownOptionsCount = question.options.size
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(stringResource(R.string.template_options_title, question.text)) },
		text = {
			Column(
				verticalArrangement = Arrangement.spacedBy(10.dp),
				modifier = Modifier
					.verticalScroll(rememberScrollState())
					.imePadding()
			) {
				if (question.options.isEmpty()) {
					Text(
						stringResource(R.string.template_options_empty),
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
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(stringResource(R.string.template_options_add_title), style = MaterialTheme.typography.titleSmall)
					OutlinedButton(
						onClick = { showHelp = !showHelp }
					) {
						Text(if (showHelp) "×" else "i")
					}
				}
				OutlinedTextField(
					value = newLabel,
					onValueChange = { newLabel = it },
					label = { Text(stringResource(R.string.template_option_label)) },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				OutlinedTextField(
					value = newValue,
					onValueChange = { newValue = it },
					label = { Text(stringResource(R.string.template_option_value)) },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)
				Button(
					onClick = {
						onAddOption(newLabel, newValue)
					},
					enabled = !loading && newLabel.isNotBlank() && newValue.isNotBlank(),
					modifier = Modifier.fillMaxWidth()
				) { Text(stringResource(R.string.template_option_add_action)) }
			}
		},
		confirmButton = {
			DialogCancelButton(text = stringResource(R.string.common_close), onClick = onDismiss)
		}
	)
	if (showHelp) {
		AlertDialog(
			onDismissRequest = { showHelp = false },
			title = { Text(stringResource(R.string.common_info)) },
			text = {
				Text(
					stringResource(R.string.template_options_add_help),
					style = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				DialogConfirmButton(
					text = stringResource(R.string.common_close),
					onClick = { showHelp = false }
				)
			}
		)
	}
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
				label = { Text(stringResource(R.string.template_option_label)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				enabled = enabled
			)
			OutlinedTextField(
				value = value,
				onValueChange = { value = it },
				label = { Text(stringResource(R.string.template_option_value)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				enabled = enabled
			)
			OutlinedTextField(
				value = orderText,
				onValueChange = { orderText = it.filter { ch -> ch.isDigit() } },
				label = { Text(stringResource(R.string.template_option_order)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				enabled = enabled
			)
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Button(
					onClick = { onSave(label, value, orderText.toIntOrNull() ?: 0) },
					enabled = enabled && label.isNotBlank() && value.isNotBlank(),
					modifier = Modifier.weight(1f)
				) { Text(stringResource(R.string.template_option_save_action), maxLines = 1) }
				TextButton(
					onClick = onDelete,
					enabled = enabled
				) { Text(stringResource(R.string.common_delete_short), maxLines = 1) }
			}
		}
	}
}
