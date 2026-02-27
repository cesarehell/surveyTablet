package com.mr.restaurant.survey.admin.coupon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.mr.restaurant.survey.admin.ui.adminCouponOfferTypeLabel
import com.mr.restaurant.survey.admin.ui.adminCouponTriggerTypeLabel
import com.mr.restaurant.survey.core.coupon.dto.CouponRuleDto
import kotlinx.coroutines.launch

private val CouponTriggerTypes = listOf("NEGATIVE", "VISITS_GE")
private val CouponOfferTypes = listOf("PERCENT", "AMOUNT", "PRODUCT")

@Composable
fun TenantCouponRulesScreen(
	tenantId: String,
	onBack: () -> Unit,
	vm: CouponRuleViewModel = hiltViewModel()
) {
	val st by vm.state.collectAsState()
	val snackbarHostState = remember { SnackbarHostState() }
	val uiScope = rememberCoroutineScope()

	var templatesExpanded by remember { mutableStateOf(false) }
	var questionsExpanded by remember { mutableStateOf(false) }
	var showCreateDialog by remember { mutableStateOf(false) }
	var createError by remember { mutableStateOf<String?>(null) }

	var triggerType by remember { mutableStateOf("NEGATIVE") }
	var selectedQuestionId by remember { mutableStateOf<String?>(null) }
	var minVisits by remember { mutableStateOf("3") }
	var offerType by remember { mutableStateOf("PERCENT") }
	var percentOff by remember { mutableStateOf("10") }
	var amountOff by remember { mutableStateOf("") }
	var product by remember { mutableStateOf("") }
	var terms by remember { mutableStateOf("") }

	fun resetCreateRuleForm() {
		createError = null
		triggerType = "NEGATIVE"
		selectedQuestionId = null
		minVisits = "3"
		offerType = "PERCENT"
		percentOff = "10"
		amountOff = ""
		product = ""
		terms = ""
	}

	LaunchedEffect(tenantId) { vm.load(tenantId) }
	LaunchedEffect(Unit) {
		vm.events.collect { event ->
			when (event) {
				is AdminUiEvent.ShowError -> {
					if (showCreateDialog) createError = event.message
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
	val selectedQuestion = questions.firstOrNull { it.id == selectedQuestionId }

	Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			SimpleTopBar(
				title = stringResource(R.string.coupon_rules_screen_title),
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
							enabled = st.templates.isNotEmpty() && !st.loading
						) {
							Text(selectedTemplate?.name ?: stringResource(R.string.coupon_rules_select_template))
						}
						DropdownMenu(expanded = templatesExpanded, onDismissRequest = { templatesExpanded = false }) {
							st.templates.forEach { tpl ->
								DropdownMenuItem(
									text = { Text(tpl.name) },
									onClick = {
										templatesExpanded = false
										vm.selectTemplate(tpl.id)
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
						enabled = st.selectedTemplateId != null && !st.contextLoading && !st.creating
					) { Text(stringResource(R.string.coupon_rules_new_rule)) }
				}

				if (st.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
				st.error?.takeIf { st.templates.isEmpty() }
					?.let { ErrorWithRetry(message = it, onRetry = { vm.load(tenantId) }) }
				if (st.contextLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
				st.contextError?.let { ErrorWithRetry(message = it, onRetry = { vm.retrySelected() }) }

				selectedTemplate?.let { tpl ->
					Text(
						stringResource(R.string.coupon_rules_template_rules_summary, tpl.name, st.rules.size),
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold
					)
				}

				if (!st.loading && st.templates.isEmpty() && st.error == null) {
					EmptyState(stringResource(R.string.coupon_rules_empty_templates), modifier = Modifier.weight(1f))
				} else if (!st.contextLoading && st.rules.isEmpty() && st.selectedTemplateId != null && st.contextError == null) {
					EmptyState(stringResource(R.string.coupon_rules_empty_rules), modifier = Modifier.weight(1f))
				} else {
					LazyColumn(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						items(st.rules, key = { it.id }) { rule ->
							val questionLabel = when {
								rule.questionId.isNullOrBlank() -> stringResource(R.string.rules_applies_all_questions)
								else -> questions.firstOrNull { it.id == rule.questionId }?.text
									?: stringResource(R.string.rules_applies_unknown_question)
							}
							CouponRuleCard(
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
			title = { Text(stringResource(R.string.coupon_rules_new_dialog_title)) },
			text = {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.verticalScroll(rememberScrollState()),
					verticalArrangement = Arrangement.spacedBy(8.dp)
				) {
					createError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
					Text(stringResource(R.string.coupon_rules_trigger))
					FilterChipWrap(
						items = CouponTriggerTypes,
						selectedItem = triggerType,
						onSelect = { triggerType = it },
						labelContent = { Text(adminCouponTriggerTypeLabel(it)) }
					)
					if (triggerType == "NEGATIVE") {
						Box {
							Button(onClick = { questionsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
								Text(
									selectedQuestion?.text ?: stringResource(R.string.coupon_rules_question_optional),
									maxLines = 1
								)
							}
							DropdownMenu(
								expanded = questionsExpanded,
								onDismissRequest = { questionsExpanded = false }) {
								DropdownMenuItem(
									text = { Text(stringResource(R.string.coupon_rules_all_questions)) },
									onClick = {
										selectedQuestionId = null
										questionsExpanded = false
									})
								questions.forEach { q ->
									DropdownMenuItem(text = { Text(q.text) }, onClick = {
										selectedQuestionId = q.id
										questionsExpanded = false
									})
								}
							}
						}
					} else {
						OutlinedTextField(
							value = minVisits,
							onValueChange = { minVisits = it },
							label = { Text(stringResource(R.string.coupon_rules_min_visits)) },
							modifier = Modifier.fillMaxWidth()
						)
					}

					Text(stringResource(R.string.coupon_rules_offer))
					FilterChipWrap(
						items = CouponOfferTypes,
						selectedItem = offerType,
						onSelect = { offerType = it },
						labelContent = { Text(adminCouponOfferTypeLabel(it)) }
					)

					when (offerType) {
						"PERCENT" -> OutlinedTextField(
							value = percentOff,
							onValueChange = { percentOff = it },
							label = { Text(stringResource(R.string.coupon_rules_percent_off)) },
							modifier = Modifier.fillMaxWidth()
						)

						"AMOUNT" -> OutlinedTextField(
							value = amountOff,
							onValueChange = { amountOff = it },
							label = { Text(stringResource(R.string.coupon_rules_amount)) },
							modifier = Modifier.fillMaxWidth()
						)

						"PRODUCT" -> OutlinedTextField(
							value = product,
							onValueChange = { product = it },
							label = { Text(stringResource(R.string.coupon_rules_product)) },
							modifier = Modifier.fillMaxWidth()
						)
					}
					OutlinedTextField(
						value = terms,
						onValueChange = { terms = it },
						label = { Text(stringResource(R.string.coupon_rules_terms_optional)) },
						modifier = Modifier.fillMaxWidth()
					)
				}
			},
			confirmButton = {
				DialogConfirmButton(
					text = if (st.creating) "Creando..." else stringResource(R.string.common_create),
					enabled = !st.creating,
					onClick = {
						vm.createRule(
							tenantId = tenantId,
							type = triggerType,
							questionId = if (triggerType == "NEGATIVE") selectedQuestionId else null,
							minVisitsInput = minVisits,
							offerType = offerType,
							percentOffInput = percentOff,
							amountOffInput = amountOff,
							productInput = product,
							termsInput = terms
						)
					}
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
private fun CouponRuleCard(
	rule: CouponRuleDto,
	questionLabel: String,
	busy: Boolean,
	onToggle: () -> Unit,
	onDelete: () -> Unit
) {
	androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.padding(12.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp)
		) {
			Text(
				"${adminCouponTriggerTypeLabel(rule.type)} · ${offerSummary(rule)}",
				fontWeight = FontWeight.Bold
			)
			if (rule.type.equals("VISITS_GE", true)) {
				Text(stringResource(R.string.coupon_rules_min_visits_value, rule.minVisits ?: rule.value.toInt()))
			}
			Text(
				stringResource(R.string.rules_applies_to, questionLabel),
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.primary
			)
			rule.terms?.takeIf { it.isNotBlank() }?.let {
				Text(it, style = MaterialTheme.typography.bodySmall)
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					Switch(checked = rule.active, onCheckedChange = { if (!busy) onToggle() }, enabled = !busy)
					Text(if (rule.active) stringResource(R.string.common_active) else stringResource(R.string.common_inactive))
				}
				TextButton(onClick = onDelete, enabled = !busy) { Text(stringResource(R.string.common_delete)) }
			}
		}
	}
}

private fun offerSummary(rule: CouponRuleDto): String = when (rule.offerType?.uppercase()) {
	"PERCENT" -> "${rule.percentOff ?: 0}% dto"
	"AMOUNT" -> "Monto ${rule.amountOff ?: 0.0}"
	"PRODUCT" -> rule.product ?: "Producto"
	else -> "Oferta"
}
