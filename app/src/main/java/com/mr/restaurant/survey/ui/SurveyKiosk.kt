package com.mr.restaurant.survey.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mr.restaurant.survey.net.dto.QuestionDto
import com.mr.restaurant.survey.net.dto.QuestionType

@Composable
fun SurveyKiosk(vm: AppViewModel) {
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

				st.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					Text("Error: ${st.error}")
				}

				st.template == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					Text("Sin template")
				}

				st.stepIndex < 0 -> WelcomeScreen(onStart = { vm.next() })
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
						SubmitScreen(onSubmit = { vm.submitAndRestart() }, message = null)
					}
				}
			}
		}
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
private fun WelcomeScreen(onStart: () -> Unit) {
	Column(
		Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text("¿Cómo estuvo tu visita hoy?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
		Spacer(Modifier.height(24.dp))
		Button(onClick = onStart) { Text("Empezar") }
	}
}

@Composable
private fun QuestionScreen(question: QuestionDto, onAnswer: (Any) -> Unit) {
	Column(
		Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			question.text,
			fontSize = 28.sp,
			fontWeight = FontWeight.Bold,
			modifier = Modifier.padding(bottom = 32.dp)
		)
		when (question.type) {
			QuestionType.LIKERT_5 -> Likert5(onSelect = onAnswer)
			QuestionType.YES_NO -> YesNoRow(onAnswer)
			QuestionType.TEXT -> TextAnswer(onAnswer)
			else -> Text("Tipo no implementado aún: ${question.type.name}")
		}
	}
}

@Composable
private fun Likert5(onSelect: (Int) -> Unit) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(24.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		val faces = listOf("😡", "🙁", "😐", "🙂", "😄")
		(1..5).forEach { score ->
			ElevatedButton(onClick = { onSelect(score) }) {
				Text(faces[score - 1], fontSize = 40.sp)
			}
		}
	}
}

@Composable
private fun YesNoRow(onAnswer: (Boolean) -> Unit) {
	Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
		ElevatedButton(onClick = { onAnswer(true) }) { Text("Sí", fontSize = 28.sp) }
		ElevatedButton(onClick = { onAnswer(false) }) { Text("No", fontSize = 28.sp) }
	}
}

@Composable
private fun TextAnswer(onAnswer: (String) -> Unit) {
	var txt by remember { mutableStateOf("") }
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		OutlinedTextField(
			value = txt,
			onValueChange = { txt = it },
			singleLine = true,
			label = { Text("Tu respuesta") }
		)
		Spacer(Modifier.height(16.dp))
		Button(onClick = { onAnswer(txt) }) { Text("Continuar") }
	}
}

@Composable
private fun SubmitScreen(onSubmit: () -> Unit, message: String?) {
	Column(
		Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Button(onClick = onSubmit) { Text("Enviar respuestas") }
		if (message != null) {
			Spacer(Modifier.height(16.dp))
			Text(message, fontSize = 20.sp)
		}
	}
}

/* ---------- Previews ---------- */

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Welcome")
@Composable
private fun PreviewWelcome() {
	MaterialTheme { Surface { WelcomeScreen(onStart = {}) } }
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
	MaterialTheme { Surface { SubmitScreen(onSubmit = {}, message = "¡Gracias!") } }
}
