package com.mr.restaurant.survey.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mr.restaurant.survey.net.dto.QuestionDto
import com.mr.restaurant.survey.net.dto.QuestionType
import com.mr.restaurant.survey.net.dto.QOptionDTO

@Composable
fun SurveyKiosk(
	vm: AppViewModel,
	onReconfigureTablet: () -> Unit = {}
) {
	val st by vm.state.collectAsState()

	var showSplash by remember { mutableStateOf(true) }
	LaunchedEffect(Unit) { kotlinx.coroutines.delay(1200); showSplash = false }

	MaterialTheme {
		Surface(Modifier.fillMaxSize()) {
			when {
				showSplash -> SplashScreen()
				st.celebrating -> CelebrationScreen()
				st.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}

				st.error != null -> ErrorScreen(
					message = st.error ?: "Error",
					onRetry = vm::retryBootstrap,
					onReconfigureTablet = onReconfigureTablet
				)

				st.template == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					Text("Sin template")
				}

				st.stepIndex < 0 -> WelcomeScreen(
					defaultTable = vm.defaultTable(),
					defaultWaiter = vm.defaultWaiter(),
					waiterOptions = vm.waiterOptions(),
					onStart = { tableNo, waiterName -> vm.beginSurvey(tableNo, waiterName) },
					onReconfigureTablet = onReconfigureTablet
				)
				else -> {
					val tpl = st.template!!
					val qs = tpl.questions.orEmpty()

					if (st.stepIndex < qs.size) {
						val q = qs[st.stepIndex]
						QuestionScreen(
							question = q,
							onAnswer = { ans ->
								vm.postAnswer(q.id, ans)
								vm.next()
							}
						)
					} else {
						SubmitScreen(onSubmit = { email, marketingOptIn ->
							vm.submitAndRestart(email, marketingOptIn)
						}, message = st.submitError)
					}
				}
			}
		}
	}
}

@Composable
private fun ErrorScreen(
	message: String,
	onRetry: () -> Unit,
	onReconfigureTablet: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text("No se pudo conectar", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
		Spacer(Modifier.height(10.dp))
		Text(message, textAlign = TextAlign.Center)
		Spacer(Modifier.height(20.dp))
		Button(onClick = onRetry) { Text("Reintentar") }
		Spacer(Modifier.height(10.dp))
		TextButton(onClick = onReconfigureTablet) { Text("Reconfigurar tablet") }
	}
}

@Composable
private fun CelebrationScreen() {
	Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		val infinite = rememberInfiniteTransition(label = "pulse")
		val scale by infinite.animateFloat(
			initialValue = 0.95f,
			targetValue = 1.05f,
			animationSpec = infiniteRepeatable(
				animation = tween(600, easing = FastOutSlowInEasing),
				repeatMode = RepeatMode.Reverse
			), label = "scale"
		)
		Text(
			"¡Gracias!",
			fontSize = 42.sp,
			fontWeight = FontWeight.Bold,
			modifier = Modifier.graphicsLayer {
				this.scaleX = scale
				this.scaleY = scale
			}
		)
	}
}

@Composable
private fun SplashScreen() {
	var started by remember { mutableStateOf(false) }
	LaunchedEffect(Unit) { started = true }

	val scale by animateFloatAsState(
		targetValue = if (started) 1f else 0.85f,
		animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
		label = "scale"
	)

	val alpha by animateFloatAsState(
		targetValue = if (started) 1f else 0f,
		animationSpec = tween(900),
		label = "alpha"
	)

	Box(
		Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
				"Restaurant Survey",
				fontSize = 36.sp,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.graphicsLayer {
					this.alpha = alpha
					this.scaleX = scale
					this.scaleY = scale
				}
			)
			Spacer(Modifier.height(16.dp))
			CircularProgressIndicator()
		}
	}
}

@Composable
private fun WelcomeScreen(
	defaultTable: String,
	defaultWaiter: String,
	waiterOptions: List<String>,
	onStart: (String, String?) -> Unit,
	onReconfigureTablet: () -> Unit
) {
	val tableNo = defaultTable

	BoxWithConstraints(Modifier.fillMaxSize()) {
		val screenWidth = this.maxWidth
		val landscape = screenWidth > maxHeight
		if (landscape) {
			Column(
				Modifier
					.fillMaxSize()
					.padding(24.dp),
				verticalArrangement = Arrangement.Center
			) {
				Row(
					Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(24.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Column(
						modifier = Modifier.weight(1f),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						Text(
							"¿Cómo estuvo tu visita hoy?",
							fontSize = 32.sp,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.pointerInput(Unit) {
								detectTapGestures(onLongPress = { onReconfigureTablet() })
							}
						)
						Text(
							"Tu opinión nos ayuda a mejorar el servicio.",
							style = MaterialTheme.typography.bodyLarge
						)
					}
					Column(
						modifier = Modifier.widthIn(max = 320.dp),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						Button(
							onClick = { onStart(tableNo, null) },
							modifier = Modifier.fillMaxWidth()
						) { Text("Empezar") }
					}
				}
			}
		} else {
			Column(
				Modifier
					.fillMaxSize()
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Top
			) {
				Spacer(Modifier.weight(1f))
				Text(
					"¿Cómo estuvo tu visita hoy?",
					fontSize = 28.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center,
					modifier = Modifier.pointerInput(Unit) {
						detectTapGestures(onLongPress = { onReconfigureTablet() })
					}
				)
				Spacer(Modifier.height(24.dp))
				Button(onClick = { onStart(tableNo, null) }) { Text("Empezar") }
				Spacer(Modifier.weight(1f))
			}
		}
	}
}

@Composable
private fun QuestionScreen(question: QuestionDto, onAnswer: (Any) -> Unit) {
	BoxWithConstraints(Modifier.fillMaxSize()) {
		val screenWidth = maxWidth
		val landscape = screenWidth > maxHeight
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = if (landscape) 40.dp else 20.dp, vertical = 20.dp),
			contentAlignment = Alignment.Center
		) {
			Column(
				Modifier
					.fillMaxWidth()
					.widthIn(max = if (landscape) 900.dp else 680.dp)
					.wrapContentHeight()
					.verticalScroll(rememberScrollState()),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
			) {
				Text(
					question.text,
					fontSize = if (landscape) 30.sp else 24.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
				when (question.type) {
						QuestionType.LIKERT_5 -> Likert5(isLandscape = landscape, maxWidth = screenWidth, onSelect = onAnswer)
					QuestionType.YES_NO -> YesNoRow(isLandscape = landscape, onAnswer = onAnswer)
					QuestionType.SINGLE -> SingleChoiceAnswer(question.options.orEmpty(), onAnswer)
					QuestionType.MULTI -> MultiChoiceAnswer(question.options.orEmpty(), onAnswer)
					QuestionType.TEXT -> TextAnswer(onAnswer)
					else -> Text("Tipo no implementado aún: ${question.type.name}")
				}
			}
		}
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Likert5(isLandscape: Boolean, maxWidth: androidx.compose.ui.unit.Dp, onSelect: (Int) -> Unit) {
	val screenWidth = maxWidth
	val buttonMinWidth = when {
		isLandscape -> 92.dp
		screenWidth < 360.dp -> 56.dp
		screenWidth < 420.dp -> 64.dp
		else -> 72.dp
	}
	val buttonMinHeight = when {
		isLandscape -> 72.dp
		screenWidth < 360.dp -> 52.dp
		else -> 60.dp
	}
	val emojiSize = when {
		isLandscape -> 38.sp
		screenWidth < 360.dp -> 28.sp
		screenWidth < 420.dp -> 32.sp
		else -> 34.sp
	}
	FlowRow(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(
			space = if (isLandscape) 20.dp else 12.dp,
			alignment = Alignment.CenterHorizontally
		),
		verticalArrangement = Arrangement.spacedBy(16.dp),
		maxItemsInEachRow = if (isLandscape) 5 else 3
	) {
		val faces = listOf("😡", "🙁", "😐", "🙂", "😄")
		(1..5).forEach { score ->
			ElevatedButton(
				onClick = { onSelect(score) },
				modifier = Modifier.defaultMinSize(minWidth = buttonMinWidth, minHeight = buttonMinHeight)
			) {
				Text(faces[score - 1], fontSize = emojiSize)
			}
		}
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YesNoRow(isLandscape: Boolean, onAnswer: (Boolean) -> Unit) {
	FlowRow(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(
			space = if (isLandscape) 24.dp else 12.dp,
			alignment = Alignment.CenterHorizontally
		),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		ElevatedButton(
			onClick = { onAnswer(true) },
			modifier = Modifier.defaultMinSize(
				minWidth = if (isLandscape) 140.dp else 120.dp,
				minHeight = if (isLandscape) 64.dp else 56.dp
			)
		) { Text("Sí", fontSize = if (isLandscape) 28.sp else 24.sp) }
		ElevatedButton(
			onClick = { onAnswer(false) },
			modifier = Modifier.defaultMinSize(
				minWidth = if (isLandscape) 140.dp else 120.dp,
				minHeight = if (isLandscape) 64.dp else 56.dp
			)
		) { Text("No", fontSize = if (isLandscape) 28.sp else 24.sp) }
	}
}

@Composable
private fun TextAnswer(onAnswer: (String) -> Unit) {
	var txt by remember { mutableStateOf("") }
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		OutlinedTextField(
			value = txt,
			onValueChange = { txt = it },
			modifier = Modifier.fillMaxWidth(0.9f),
			singleLine = true,
			label = { Text("Tu respuesta") }
		)
		Spacer(Modifier.height(16.dp))
		Button(onClick = { onAnswer(txt) }) { Text("Continuar") }
	}
}

@Composable
private fun SingleChoiceAnswer(options: List<QOptionDTO>, onAnswer: (String) -> Unit) {
	if (options.isEmpty()) {
		Text("Esta pregunta no tiene opciones configuradas.")
		return
	}

	Column(
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		options.sortedBy { it.oOrder }.forEach { option ->
			ElevatedButton(
				onClick = { onAnswer(option.value) },
				modifier = Modifier.fillMaxWidth(0.85f)
			) {
				Text(option.label, fontSize = 22.sp)
			}
		}
	}
}

@Composable
private fun MultiChoiceAnswer(options: List<QOptionDTO>, onAnswer: (List<String>) -> Unit) {
	if (options.isEmpty()) {
		Text("Esta pregunta no tiene opciones configuradas.")
		return
	}

	var selected by remember { mutableStateOf(setOf<String>()) }
	Column(
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		options.sortedBy { it.oOrder }.forEach { option ->
			val isSelected = option.value in selected
			FilterChip(
				selected = isSelected,
				onClick = {
					selected = if (isSelected) selected - option.value else selected + option.value
				},
				label = { Text(option.label, fontSize = 18.sp) }
			)
		}

		Button(
			onClick = { onAnswer(selected.toList()) },
			enabled = selected.isNotEmpty()
		) {
			Text("Continuar")
		}
	}
}

@Composable
private fun SubmitScreen(onSubmit: (String?, Boolean) -> Unit, message: String?) {
	var email by remember { mutableStateOf("") }
	var marketingOptIn by remember { mutableStateOf(false) }
	var emailError by remember { mutableStateOf<String?>(null) }

	fun submitWithValidation(forceWithoutEmail: Boolean = false) {
		val trimmed = email.trim()
		if (!forceWithoutEmail && trimmed.isBlank()) {
			emailError = "Ingresa un correo o usa \"Enviar sin correo\""
			return
		}
		val finalEmail = if (forceWithoutEmail || trimmed.isBlank()) null else trimmed
		if (finalEmail != null && !isValidEmail(finalEmail)) {
			emailError = "Ingresa un correo válido o envía sin correo"
			return
		}
		emailError = null
		onSubmit(finalEmail, marketingOptIn)
	}

	BoxWithConstraints(Modifier.fillMaxSize()) {
		val landscape = maxWidth > maxHeight
		Column(
			Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.padding(24.dp),
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			if (landscape) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(24.dp),
					verticalAlignment = Alignment.Top
				) {
					Column(modifier = Modifier.weight(1f)) {
						Text("Finaliza tu experiencia", fontSize = 26.sp, fontWeight = FontWeight.Bold)
						Spacer(Modifier.height(8.dp))
						Text("Si lo compartes, podremos enviarte agradecimiento o cupón.", fontSize = 14.sp)
					}
					SubmitForm(
						email = email,
						onEmailChange = {
							email = it
							if (emailError != null) emailError = null
						},
						marketingOptIn = marketingOptIn,
						onMarketingChange = { marketingOptIn = it },
						emailError = emailError,
						onSubmit = { submitWithValidation() },
						onSubmitWithoutEmail = { submitWithValidation(forceWithoutEmail = true) },
						modifier = Modifier.weight(1f)
					)
				}
			} else {
				Text("Finaliza tu experiencia", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
				Spacer(Modifier.height(16.dp))
				SubmitForm(
					email = email,
					onEmailChange = {
						email = it
						if (emailError != null) emailError = null
					},
					marketingOptIn = marketingOptIn,
					onMarketingChange = { marketingOptIn = it },
					emailError = emailError,
					onSubmit = { submitWithValidation() },
					onSubmitWithoutEmail = { submitWithValidation(forceWithoutEmail = true) },
					modifier = Modifier.fillMaxWidth(0.9f)
				)
			}
			if (message != null) {
				Spacer(Modifier.height(16.dp))
				Text(message, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
			}
		}
	}
}

@Composable
private fun SubmitForm(
	email: String,
	onEmailChange: (String) -> Unit,
	marketingOptIn: Boolean,
	onMarketingChange: (Boolean) -> Unit,
	emailError: String?,
	onSubmit: () -> Unit,
	onSubmitWithoutEmail: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
		OutlinedTextField(
			value = email,
			onValueChange = onEmailChange,
			modifier = Modifier.fillMaxWidth(),
			label = { Text("Correo (opcional)") },
			singleLine = true,
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
		)
		Spacer(Modifier.height(8.dp))
		Text(
			"Si lo compartes, podremos enviarte agradecimiento o cupón.",
			fontSize = 14.sp,
			textAlign = TextAlign.Center
		)
		Spacer(Modifier.height(12.dp))
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Start
		) {
			Checkbox(
				checked = marketingOptIn,
				onCheckedChange = onMarketingChange
			)
			Text("Acepto recibir promociones por correo")
		}
		if (emailError != null) {
			Spacer(Modifier.height(8.dp))
			Text(emailError, color = MaterialTheme.colorScheme.error)
		}
		Spacer(Modifier.height(16.dp))
		Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text("Enviar respuestas") }
		Spacer(Modifier.height(8.dp))
		TextButton(onClick = onSubmitWithoutEmail) {
			Text("Enviar sin correo")
		}
	}
}

private fun isValidEmail(value: String): Boolean {
	val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
	return emailRegex.matches(value)
}

/* ---------- Previews ---------- */

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Welcome")
@Composable
private fun PreviewWelcome() {
	MaterialTheme {
		Surface {
			WelcomeScreen(
				defaultTable = "A7",
				defaultWaiter = "ERIKA",
				waiterOptions = listOf("ERIKA", "LUIS"),
				onStart = { _, _ -> },
				onReconfigureTablet = {}
			)
		}
	}
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Likert 5")
@Composable
private fun PreviewLikert() {
	MaterialTheme {
		Surface {
			QuestionScreen(
				question = QuestionDto(
					id = "q1",
					type = QuestionType.LIKERT_5,
					text = "¿Cómo califica la comida?"
				),
				onAnswer = {}
			)
		}
	}
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Sí/No")
@Composable
private fun PreviewYesNo() {
	MaterialTheme {
		Surface {
			QuestionScreen(
				question = QuestionDto(
					id = "q4",
					type = QuestionType.YES_NO,
					text = "¿La orden llegó completa?",
					required = false
				),
				onAnswer = {}
			)
		}
	}
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Texto")
@Composable
private fun PreviewText() {
	MaterialTheme {
		Surface {
			QuestionScreen(
				question = QuestionDto(
					id = "q5",
					type = QuestionType.TEXT,
					text = "¿Algún comentario adicional?",
					required = false
				),
				onAnswer = {}
			)
		}
	}
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Submit")
@Composable
private fun PreviewSubmit() {
	MaterialTheme { Surface { SubmitScreen(onSubmit = { _, _ -> }, message = "¡Gracias!") } }
}
