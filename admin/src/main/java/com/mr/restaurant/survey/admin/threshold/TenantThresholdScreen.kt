package com.mr.restaurant.survey.admin.threshold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mr.restaurant.survey.admin.R
import com.mr.restaurant.survey.admin.ui.AdminUiEvent
import com.mr.restaurant.survey.admin.ui.DialogCancelButton
import com.mr.restaurant.survey.admin.ui.DialogConfirmButton
import com.mr.restaurant.survey.admin.ui.EmptyState
import com.mr.restaurant.survey.admin.ui.ErrorBanner
import com.mr.restaurant.survey.admin.ui.ErrorWithRetry
import com.mr.restaurant.survey.admin.ui.FilterChipWrap
import com.mr.restaurant.survey.admin.ui.SimpleTopBar
import com.mr.restaurant.survey.admin.ui.adminThresholdTypeLabel
import com.mr.restaurant.survey.core.threshold.dto.ThresholdRuleDto
import kotlinx.coroutines.launch

private val ThresholdTypes = listOf("NEGATIVE", "LT", "LE", "EQ")

@Composable
fun TenantThresholdsScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: ThresholdViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }
	val uiScope = rememberCoroutineScope()

	var templatesExpanded by remember { mutableStateOf(false) }
	var showCreateDialog by remember { mutableStateOf(false) }
	var createDialogError by remember { mutableStateOf<String?>(null) }
	var newType by remember { mutableStateOf("NEGATIVE") }
	var newValue by remember { mutableStateOf("2") }
	var newCooldown by remember { mutableStateOf("10") }
	var selectedQuestionId by remember { mutableStateOf<String?>(null) }
	var questionsExpanded by remember { mutableStateOf(false) }

	fun resetCreateRuleForm() {
		createDialogError = null
		newType = "NEGATIVE"
		newValue = "2"
		newCooldown = "10"
		selectedQuestionId = null
	}

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> {
					if (showCreateDialog) createDialogError = event.message
					else uiScope.launch { snackbarHostState.showSnackbar(event.message) }
				}

				is AdminUiEvent.ShowSuccess -> uiScope.launch { snackbarHostState.showSnackbar(event.message) }
				AdminUiEvent.CloseDialog -> {
					showCreateDialog = false
					resetCreateRuleForm()
				}

				is AdminUiEvent.NavigateToTemplateDetail -> Unit
			}
		}
	}

	val selectedTemplate = st.templates.firstOrNull { it.id == st.selectedTemplateId }
	val questions = st.templateFull?.questions.orEmpty()
	val thresholdQuestions = questions.filterNot { it.type == "TEXT" }
	val selectedQuestion = thresholdQuestions.firstOrNull { it.id == selectedQuestionId }
	val selectedQuestionIsYesNo = selectedQuestion?.type == "YESNO"
	val availableThresholdTypes = if (selectedQuestionIsYesNo) listOf("EQ") else ThresholdTypes

	LaunchedEffect(thresholdQuestions, selectedQuestionId) {
		if (selectedQuestionId != null && thresholdQuestions.none { it.id == selectedQuestionId }) {
			selectedQuestionId = null
		}
	}

	Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			SimpleTopBar(
				title = stringResource(R.string.thresholds_screen_title),
				subtitle = tenantId,
				onBack = onBack
			)

			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					Box(modifier = Modifier.weight(1f)) {
						Button(
							modifier = Modifier.fillMaxWidth(),
							onClick = { templatesExpanded = true },
							enabled = st.templates.isNotEmpty() && !st.loading,
							colors = androidx.compose.material3.ButtonDefaults.buttonColors(
								containerColor = MaterialTheme.colorScheme.surface,
								contentColor = MaterialTheme.colorScheme.primary
							)
						) {
							Text(
								text = selectedTemplate?.name ?: "Seleccionar template",
								maxLines = 1,
								overflow = TextOverflow.Ellipsis
							)
						}
						DropdownMenu(
							expanded = templatesExpanded,
							onDismissRequest = { templatesExpanded = false }
						) {
							st.templates.forEach { template ->
								DropdownMenuItem(
									text = { Text(template.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
									onClick = {
										templatesExpanded = false
										vm.selectTemplate(template.id)
									}
								)
							}
						}
					}

					Button(
						onClick = {
							resetCreateRuleForm()
							showCreateDialog = true
						},
						enabled = st.selectedTemplateId != null && !st.contextLoading && !st.creating,
						colors = androidx.compose.material3.ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.secondary,
							contentColor = MaterialTheme.colorScheme.onSecondary
						)
					) { Text("Nueva regla") }
				}

				if (st.loading) {
					LinearProgressIndicator(Modifier.fillMaxWidth())
				}

				st.error?.takeIf { st.templates.isEmpty() }?.let { message ->
					ErrorWithRetry(message = message, onRetry = { vm.load(tenantId) })
				}

				selectedTemplate?.let { template ->
					Card(
						Modifier.fillMaxWidth(),
						colors = androidx.compose.material3.CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.secondaryContainer
						)
					) {
						Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
							Text(
								template.name,
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.primary,
								maxLines = 2,
								overflow = TextOverflow.Ellipsis
							)
							Text(
								"Status: ${template.status} · Scope: ${template.scope ?: "-"}",
								style = MaterialTheme.typography.bodySmall
							)
							Text(
								"Preguntas: ${questions.size} · Reglas: ${st.rules.size}",
								style = MaterialTheme.typography.bodySmall
							)
						}
					}
				}

				if (st.contextLoading) {
					LinearProgressIndicator(Modifier.fillMaxWidth())
				}

				st.contextError?.let { message ->
					ErrorWithRetry(message = message, onRetry = vm::retrySelected)
				}

				if (!st.loading && st.templates.isEmpty() && st.error == null) {
					EmptyState(
						message = stringResource(R.string.thresholds_empty_templates),
						modifier = Modifier.weight(1f)
					)
				} else if (!st.contextLoading && st.rules.isEmpty() && st.selectedTemplateId != null && st.contextError == null) {
					EmptyState(
						message = "No hay reglas para este template.",
						modifier = Modifier.weight(1f)
					)
				} else {
					LazyColumn(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(8.dp)
						) {
							items(st.rules, key = { it.id }) { rule ->
								val resolvedLabel = rule.resolvedQuestionLabel?.trim().orEmpty()
								val resolvedQuestionId = rule.resolvedQuestionId?.trim().orEmpty()
								val questionLabel = when {
									resolvedLabel.isNotBlank() -> resolvedLabel
									resolvedQuestionId.isBlank() -> stringResource(R.string.rules_applies_all_questions)
									else -> questions.firstOrNull { it.id.trim().equals(resolvedQuestionId, ignoreCase = true) }?.text
										?: stringResource(R.string.rules_applies_unknown_question)
								}
							ThresholdRuleCard(
								rule = rule,
								questionLabel = questionLabel,
								busy = rule.id in st.busyRuleIds,
								onToggle = { vm.toggleRule(rule) },
								onDelete = { vm.deleteRule(rule.id) }
							)
						}
					}
				}
			}
		}
	}

	if (showCreateDialog) {
		AlertDialog(
			onDismissRequest = {
				showCreateDialog = false
				resetCreateRuleForm()
			},
			title = { Text(stringResource(R.string.thresholds_new_rule_title)) },
			text = {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					createDialogError?.let { ErrorBanner(it) }
					Text(
						selectedTemplate?.name ?: stringResource(R.string.thresholds_no_selected_survey),
						style = MaterialTheme.typography.bodySmall,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis
					)

					Text(stringResource(R.string.thresholds_label_type), style = MaterialTheme.typography.labelMedium)
						FilterChipWrap(
							items = availableThresholdTypes,
							selectedItem = newType,
							onSelect = {
								newType = it
								if (it == "NEGATIVE") {
									newValue = "2"
								}
								createDialogError = null
							},
							labelContent = { Text(adminThresholdTypeLabel(it)) }
						)
					Text(stringResource(R.string.thresholds_condition_help), style = MaterialTheme.typography.bodySmall)
					if (selectedQuestionIsYesNo) {
						Text(
							stringResource(R.string.thresholds_label_answer),
							style = MaterialTheme.typography.labelMedium
						)
						FilterChipWrap(
							items = listOf("1", "0"),
							selectedItem = newValue,
							onSelect = { value ->
								newType = "EQ"
								newValue = value
								createDialogError = null
							},
							labelContent = {
								Text(
									if (it == "1") stringResource(R.string.common_yes)
									else stringResource(R.string.common_no)
								)
							}
						)
						Text(
							stringResource(R.string.thresholds_yesno_help),
							style = MaterialTheme.typography.bodySmall
						)
					} else if (newType == "NEGATIVE") {
						OutlinedTextField(
							value = "2",
							onValueChange = {},
							label = { Text("Valor") },
							singleLine = true,
							enabled = false
						)
						Text(
							"Para alerta negativa se usa automáticamente <= 2.",
							style = MaterialTheme.typography.bodySmall
						)
					} else {
						OutlinedTextField(
							value = newValue,
							onValueChange = {
								newValue = it
								createDialogError = null
							},
							label = { Text("Valor") },
							singleLine = true,
							keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
						)
					}

					OutlinedTextField(
						value = newCooldown,
						onValueChange = {
							newCooldown = it
							createDialogError = null
						},
						label = { Text("Cooldown (min)") },
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
					)

					Box(modifier = Modifier.fillMaxWidth()) {
						Button(
							modifier = Modifier.fillMaxWidth(),
							onClick = { questionsExpanded = true },
							enabled = thresholdQuestions.isNotEmpty()
						) {
							Text(
								text = selectedQuestion?.text ?: "Todas las preguntas",
								maxLines = 1,
								overflow = TextOverflow.Ellipsis
							)
						}
						DropdownMenu(
							expanded = questionsExpanded,
							onDismissRequest = { questionsExpanded = false }
						) {
							DropdownMenuItem(
								text = { Text("Todas las preguntas") },
								onClick = {
									selectedQuestionId = null
									questionsExpanded = false
								}
							)
							thresholdQuestions.forEach { q ->
								DropdownMenuItem(
									text = { Text(q.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
									onClick = {
										selectedQuestionId = q.id
										if (q.type == "YESNO") {
											newType = "EQ"
											if (newValue != "0" && newValue != "1") newValue = "1"
										} else if (newType == "NEGATIVE") {
											newValue = "2"
										}
										questionsExpanded = false
									}
								)
							}
						}
					}
				}
			},
			confirmButton = {
				DialogConfirmButton(
					text = if (st.creating) "Creando…" else "Crear",
					enabled = !st.creating && st.selectedTemplateId != null,
					onClick = {
						createDialogError = null
						vm.createRule(
							tenantId = tenantId,
							type = newType,
							valueInput = newValue,
							cooldownInput = newCooldown,
							questionId = selectedQuestionId
						)
					},
				)
			},
			dismissButton = {
				DialogCancelButton(onClick = {
					showCreateDialog = false
					resetCreateRuleForm()
				})
			}
		)
	}
}

@Composable
private fun ThresholdRuleCard(
	rule: ThresholdRuleDto,
	questionLabel: String,
	busy: Boolean,
	onToggle: () -> Unit,
	onDelete: () -> Unit
) {
	Card(
		Modifier.fillMaxWidth(),
		colors = androidx.compose.material3.CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		),
		elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
	) {
		Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Column(Modifier.weight(1f)) {
					Text(
						"${adminThresholdTypeLabel(rule.type ?: "-")} · ${rule.value}",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.SemiBold,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis
					)
					Text(
						"Cooldown: ${rule.cooldownMin} min",
						style = MaterialTheme.typography.bodySmall
					)
				}
				Surface(
					color = if (rule.active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
					shape = MaterialTheme.shapes.small
				) {
					Text(
						if (rule.active) "ACTIVA" else "INACTIVA",
						modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
						style = MaterialTheme.typography.labelSmall,
						fontWeight = FontWeight.Bold,
						color = if (rule.active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(
							alpha = 0.7f
						)
					)
				}
			}
			Text(
				stringResource(R.string.rules_applies_to, questionLabel),
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.primary,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
			rule.location?.let {
				Text(
					"Location: ${it.name}",
					style = MaterialTheme.typography.bodySmall,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
			rule.lastTriggeredAt?.let {
				Text("Última activación: $it", style = MaterialTheme.typography.bodySmall)
			}
			HorizontalDivider()
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				TextButton(onClick = onToggle, enabled = !busy) {
					Text(if (rule.active) "Desactivar" else "Activar")
				}
				TextButton(onClick = onDelete, enabled = !busy) {
					Text("Eliminar", color = MaterialTheme.colorScheme.error)
				}
			}
		}
	}
}
