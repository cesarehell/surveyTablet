package com.mr.restaurant.survey.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mr.restaurant.survey.R
import com.mr.restaurant.survey.core.common.DisplayDateTimeFormatter
import com.mr.restaurant.survey.net.dto.CouponViewDto
import com.mr.restaurant.survey.net.dto.QOptionDTO
import com.mr.restaurant.survey.net.dto.QuestionDto
import com.mr.restaurant.survey.net.dto.QuestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TabletAccent = Color(0xFF6D52CC)
private val TabletAccentSoft = Color(0xFFE9E2FF)
private val TabletBgTop = Color(0xFFFAF9FF)
private val TabletBgBottom = Color(0xFFF3F0FF)

@Composable
fun SurveyKiosk(
	vm: AppViewModel,
	onReconfigureTablet: () -> Unit = {},
	onCloseApp: () -> Unit = {}
) {
	val st by vm.state.collectAsState()

	var showSplash by remember { mutableStateOf(true) }
	LaunchedEffect(Unit) { kotlinx.coroutines.delay(1200); showSplash = false }

	MaterialTheme {
		Surface(Modifier.fillMaxSize()) {
			when {
				showSplash -> {
					SplashScreen()
				}

				st.submitOutcome != null -> {
					SubmitOutcomeScreen(
						outcome = st.submitOutcome!!,
						onContinue = vm::dismissSubmitOutcome
					)
				}

				st.celebrating -> {
					CelebrationScreen()
				}

				st.loading -> {
					Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						CircularProgressIndicator()
					}
				}

				st.error != null -> {
					if (isNoSurveyConfiguredError(st.error)) {
						NoActiveSurveyScreen(
							message = st.error ?: stringResource(R.string.survey_no_template),
							onRetry = vm::retryBootstrap
						)
					} else {
						ErrorScreen(
							message = st.error ?: "",
							onRetry = vm::retryBootstrap,
							onReconfigureTablet = onReconfigureTablet,
							onUseOffline = if (st.canUseOffline) vm::useOfflineMode else null,
							pendingSyncCount = st.pendingSyncCount,
							onCloseApp = onCloseApp
						)
					}
				}

				st.template == null -> {
					Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
						Text(stringResource(R.string.survey_no_template))
					}
				}

				st.stepIndex < 0 -> {
					WelcomeScreen(
						defaultTable = vm.defaultTable(),
						hasQuestions = st.template?.questions?.isNotEmpty() == true,
						offlineMode = st.offlineMode,
						pendingSyncCount = st.pendingSyncCount,
						onStart = { tableNo, waiterName -> vm.beginSurvey(tableNo, waiterName) },
						onReconfigureTablet = onReconfigureTablet
					)
				}

				else -> {
					val tpl = st.template!!
					val qs = tpl.questions.orEmpty()
						.sortedWith(
							compareBy<com.mr.restaurant.survey.net.dto.QuestionDto> { it.qorder ?: Int.MAX_VALUE }
								.thenBy { it.id }
						)

					if (qs.isEmpty()) {
						EmptySurveyScreen(onBack = vm::returnToWelcome)
					} else if (st.stepIndex < qs.size) {
						val q = qs[st.stepIndex]
						QuestionScreen(
							question = q,
							stepNumber = st.stepIndex + 1,
							totalQuestions = qs.size,
							onExit = vm::returnToWelcome,
							onSkipOptional = { vm.next() },
							onSkipInvalid = { vm.next() },
							onAnswer = { ans ->
								vm.postAnswer(q.id, ans)
								vm.next()
							}
						)
					} else {
						SubmitScreen(offlineMode = st.offlineMode, onSubmit = { email, marketingOptIn ->
							vm.submitAndRestart(email, marketingOptIn)
						}, message = st.submitError)
					}
				}
			}
		}
	}
}

private fun isNoSurveyConfiguredError(message: String?): Boolean {
	val m = message?.lowercase() ?: return false
	return m.contains("no hay encuesta activa") ||
			m.contains("no hay templates publicados") ||
			m.contains("sin encuesta disponible")
}

@Composable
private fun NoActiveSurveyScreen(
	message: String,
	onRetry: () -> Unit
) {
	Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Card(
			modifier = Modifier
				.padding(24.dp)
				.widthIn(max = 560.dp)
		) {
			Column(
				modifier = Modifier.padding(22.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(14.dp)
			) {
				NoSurveyIllustration()
				Surface(
					color = MaterialTheme.colorScheme.secondaryContainer,
					shape = MaterialTheme.shapes.small
				) {
					Text(
						stringResource(R.string.survey_no_template_badge),
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSecondaryContainer,
						modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
					)
				}
				Text(
					stringResource(R.string.survey_no_template_title),
					fontSize = 24.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
				Text(
					stringResource(R.string.survey_no_template_message),
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Card(
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
					)
				) {
					Text(
						message,
						textAlign = TextAlign.Center,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
					)
				}
				Spacer(Modifier.height(2.dp))
				Button(onClick = onRetry) {
					Text(stringResource(R.string.action_retry))
				}
				Text(
					stringResource(R.string.survey_no_template_retry_hint),
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
private fun NoSurveyIllustration() {
	val primary = MaterialTheme.colorScheme.primary
	val primarySoft = MaterialTheme.colorScheme.primaryContainer
	val outline = MaterialTheme.colorScheme.outlineVariant
	Canvas(modifier = Modifier.size(92.dp)) {
		// Soft halo
		drawCircle(
			color = primarySoft.copy(alpha = 0.7f),
			radius = size.minDimension * 0.48f
		)
		// Outer ring
		drawCircle(
			color = outline.copy(alpha = 0.7f),
			radius = size.minDimension * 0.44f,
			style = Stroke(width = 3.dp.toPx())
		)
		// Clipboard body
		val w = size.width * 0.46f
		val h = size.height * 0.54f
		val left = (size.width - w) / 2f
		val top = (size.height - h) / 2f + 4.dp.toPx()
		drawRoundRect(
			color = Color.White.copy(alpha = 0.95f),
			topLeft = Offset(left, top),
			size = Size(w, h),
			cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
		)
		drawRoundRect(
			color = outline.copy(alpha = 0.45f),
			topLeft = Offset(left, top),
			size = Size(w, h),
			cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
			style = Stroke(width = 1.5.dp.toPx())
		)
		// Clipboard tab
		drawRoundRect(
			color = primary,
			topLeft = Offset(size.width * 0.41f, top - 6.dp.toPx()),
			size = Size(size.width * 0.18f, 10.dp.toPx()),
			cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
		)
		// Placeholder lines
		val lineLeft = left + 9.dp.toPx()
		val lineRight = left + w - 9.dp.toPx()
		var y = top + 13.dp.toPx()
		repeat(3) {
			drawLine(
				color = primary.copy(alpha = if (it == 0) 0.8f else 0.45f),
				start = Offset(lineLeft, y),
				end = Offset(lineRight - if (it == 2) 16.dp.toPx() else 0f, y),
				strokeWidth = 2.dp.toPx()
			)
			y += 10.dp.toPx()
		}
	}
}

@Composable
private fun EmptySurveyScreen(onBack: () -> Unit) {
	Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Column(
			modifier = Modifier
				.padding(24.dp)
				.widthIn(max = 520.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text(
				stringResource(R.string.survey_empty_title),
				fontSize = 26.sp,
				fontWeight = FontWeight.Bold,
				textAlign = TextAlign.Center
			)
			Text(
				stringResource(R.string.survey_empty_message),
				textAlign = TextAlign.Center
			)
			Button(onClick = onBack) { Text(stringResource(R.string.common_back)) }
		}
	}
}

@Composable
private fun SubmitOutcomeScreen(
	outcome: SubmitOutcome,
	onContinue: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp)
			.verticalScroll(rememberScrollState()),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			if (outcome.offlineQueued) stringResource(R.string.submit_outcome_saved_title)
			else stringResource(R.string.submit_outcome_thanks_title),
			fontSize = 28.sp,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)
		Spacer(Modifier.height(12.dp))
		outcome.visitCount?.let {
			Text(stringResource(R.string.submit_outcome_visit_count, it), textAlign = TextAlign.Center)
			Spacer(Modifier.height(12.dp))
		}

		when {
			outcome.offlineQueued -> {
				Text(
					stringResource(R.string.submit_outcome_offline_message),
					textAlign = TextAlign.Center
				)
			}

			outcome.coupon != null -> CouponResultCard(outcome.coupon)
			!outcome.couponStatus.isNullOrBlank() -> Text(
				stringResource(
					R.string.submit_outcome_coupon_status,
					couponStatusLabel(outcome.couponStatus)
				),
				textAlign = TextAlign.Center
			)

			else -> Text(stringResource(R.string.submit_outcome_no_coupon), textAlign = TextAlign.Center)
		}

		Spacer(Modifier.height(20.dp))
		Button(onClick = onContinue) { Text(stringResource(R.string.submit_outcome_new_survey)) }
	}
}

@Composable
private fun CouponResultCard(coupon: CouponViewDto) {
	Card(
		Modifier
			.fillMaxWidth()
			.widthIn(max = 520.dp)
	) {
		Column(
			Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				stringResource(R.string.coupon_emitted_title),
				fontWeight = FontWeight.Bold,
				fontSize = 20.sp,
				textAlign = TextAlign.Center
			)
			coupon.code?.let { code ->
				Text(code, fontWeight = FontWeight.Bold, fontSize = 26.sp, textAlign = TextAlign.Center)
			}
			coupon.qr?.takeIf { it.isNotBlank() }?.let { qrPayload ->
				val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = qrPayload) {
					value = withContext(Dispatchers.Default) {
						buildQrBitmapOrNull(qrPayload, sizePx = 420)
					}
				}
				if (qrBitmap != null) {
					Image(
						bitmap = qrBitmap!!.asImageBitmap(),
						contentDescription = stringResource(R.string.coupon_qr_content_description),
						modifier = Modifier
							.size(168.dp)
					)
				}
			}
			Text(couponOfferLabel(coupon), textAlign = TextAlign.Center)
			coupon.expiresAt?.let {
				Text(
					stringResource(R.string.coupon_expires_at, DisplayDateTimeFormatter.humanDateTime(it)),
					textAlign = TextAlign.Center
				)
			}
			coupon.terms?.takeIf { it.isNotBlank() }?.let {
				Text(
					it,
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodySmall
				)
			}
		}
	}
}

private fun buildQrBitmapOrNull(content: String, sizePx: Int): Bitmap? {
	return runCatching {
		val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
		val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
		for (x in 0 until sizePx) {
			for (y in 0 until sizePx) {
				bitmap.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
			}
		}
		bitmap
	}.getOrNull()
}

private fun couponOfferLabel(coupon: CouponViewDto): String {
	return when (coupon.offerType?.uppercase()) {
		"PERCENT" -> {
			val pct = coupon.percentOff ?: return "Cupón disponible"
			"$pct% de descuento"
		}

		"AMOUNT" -> coupon.amountOff?.let { "Descuento de $it" } ?: "Descuento aplicado"
		"PRODUCT" -> coupon.product?.let { "Producto: $it" } ?: "Producto promocional"
		else -> "Cupón disponible"
	}
}

private fun couponStatusLabel(status: String): String = when (status.uppercase()) {
	"PLAN_REQUIRED" -> "Límite diario alcanzado"
	"COUPONS_DISABLED" -> "Cupones deshabilitados"
	"NO_EMAIL" -> "Sin correo para emitir cupón"
	"EMAIL_REQUIRED" -> "Esta regla de cupón requiere correo"
	"ALREADY_ISSUED_30D" -> "Ya recibiste un cupón recientemente"
	else -> status
}

@Composable
private fun ErrorScreen(
	message: String,
	onRetry: () -> Unit,
	onReconfigureTablet: () -> Unit,
	onUseOffline: (() -> Unit)?,
	pendingSyncCount: Int,
	onCloseApp: () -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Text(
			stringResource(R.string.error_connect_title),
			fontSize = 24.sp,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)
		Spacer(Modifier.height(10.dp))
		Text(message, textAlign = TextAlign.Center)
		if (pendingSyncCount > 0) {
			Spacer(Modifier.height(8.dp))
			Text(stringResource(R.string.pending_sync_count, pendingSyncCount), textAlign = TextAlign.Center)
		}
		Spacer(Modifier.height(20.dp))
		Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
		if (onUseOffline != null) {
			Spacer(Modifier.height(10.dp))
			OutlinedButton(onClick = onUseOffline) { Text(stringResource(R.string.action_offline_mode)) }
		}
		Spacer(Modifier.height(10.dp))
		TextButton(onClick = onReconfigureTablet) { Text(stringResource(R.string.action_reconfigure_tablet)) }
		TextButton(onClick = onCloseApp) { Text(stringResource(R.string.action_close_app)) }
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
			stringResource(R.string.celebration_thanks),
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
				stringResource(R.string.survey_splash_title),
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
	hasQuestions: Boolean,
	offlineMode: Boolean,
	pendingSyncCount: Int,
	onStart: (String, String?) -> Unit,
	onReconfigureTablet: () -> Unit
) {
	val tableNo = defaultTable
	val startSurvey = { if (hasQuestions) onStart(tableNo, null) }
	val configuration = LocalConfiguration.current
	val screenWidth = configuration.screenWidthDp.dp
	val landscape = configuration.screenWidthDp > configuration.screenHeightDp

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(
				Brush.verticalGradient(
					listOf(
						TabletBgTop,
						Color(0xFFF8F5FF),
						TabletBgBottom
					)
					)
				)
			.padding(horizontal = if (landscape) 36.dp else 24.dp, vertical = 20.dp)
	) {
		Column(
			modifier = Modifier.fillMaxSize(),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
				Spacer(Modifier.weight(if (landscape) 0.55f else 0.7f))
				Image(
					painter = painterResource(id = R.drawable.mr_restaurant_logo),
					contentDescription = stringResource(R.string.welcome_brand_short),
					contentScale = ContentScale.Crop,
					modifier = Modifier
						.size(if (landscape) 90.dp else 84.dp)
						.clip(CircleShape)
				)
				Spacer(Modifier.height(if (landscape) 20.dp else 26.dp))

				Text(
					stringResource(R.string.welcome_hero_pretitle),
					fontSize = if (landscape) 24.sp else 22.sp,
					textAlign = TextAlign.Center,
					color = Color(0xFF9A9988)
				)
				Spacer(Modifier.height(6.dp))
				Text(
					stringResource(R.string.welcome_hero_title_line1),
					fontSize = if (landscape) 38.sp else 34.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center,
					color = Color(0xFF1F1A3A)
				)
				Text(
					stringResource(R.string.welcome_hero_title_line2),
					fontSize = if (landscape) 38.sp else 34.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center,
					color = TabletAccent
				)
				Spacer(Modifier.height(12.dp))
				Text(
					stringResource(R.string.welcome_hero_subtitle),
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center,
					color = Color(0xFF6F7393)
				)

				Spacer(Modifier.height(if (landscape) 18.dp else 24.dp))
				WelcomeStatusMessages(
					offlineMode = offlineMode,
					pendingSyncCount = pendingSyncCount,
					hasQuestions = hasQuestions,
					centered = true
				)

				Spacer(Modifier.height(18.dp))
				Button(
					onClick = { startSurvey() },
					enabled = hasQuestions,
					modifier = Modifier
						.fillMaxWidth(if (landscape) 0.55f else 1f)
						.height(64.dp),
					shape = MaterialTheme.shapes.extraLarge,
					colors = ButtonDefaults.buttonColors(
						containerColor = TabletAccent,
						contentColor = Color.White,
						disabledContainerColor = Color(0xFFE6E2DB),
						disabledContentColor = Color(0xFF9E9A93)
					)
				) {
					Text(
						stringResource(R.string.welcome_start_survey),
						fontSize = 22.sp,
						fontWeight = FontWeight.Bold
					)
				}

				Spacer(Modifier.height(14.dp))
				Text(
					stringResource(R.string.welcome_duration_hint),
					style = MaterialTheme.typography.labelLarge,
					letterSpacing = 1.sp,
					color = Color(0xFFB2B6C3),
					textAlign = TextAlign.Center
				)

				Spacer(Modifier.weight(if (landscape) 0.75f else 1f))
				Spacer(Modifier.height(18.dp))
				Text(
					stringResource(R.string.welcome_brand_footer),
					style = MaterialTheme.typography.bodySmall,
					color = Color(0xFF9892B3),
					modifier = Modifier.pointerInput(Unit) {
						detectTapGestures(onLongPress = { onReconfigureTablet() })
					}
				)
				Spacer(Modifier.height(8.dp))
		}
	}
}

@Composable
private fun WelcomeStatusMessages(
	offlineMode: Boolean,
	pendingSyncCount: Int,
	hasQuestions: Boolean,
	centered: Boolean
) {
	val alignment = if (centered) TextAlign.Center else TextAlign.Start
	val bodyStyle = MaterialTheme.typography.bodyMedium
	if (offlineMode) {
		Text(
			stringResource(R.string.welcome_offline_hint),
			style = bodyStyle,
			textAlign = alignment,
			color = MaterialTheme.colorScheme.primary
		)
		Spacer(Modifier.height(12.dp))
	}
	if (pendingSyncCount > 0) {
		Text(
			stringResource(R.string.pending_sync_count, pendingSyncCount),
			style = bodyStyle,
			textAlign = alignment
		)
		Spacer(Modifier.height(12.dp))
	}
	if (!hasQuestions) {
		Text(
			stringResource(R.string.welcome_no_questions),
			style = bodyStyle,
			textAlign = alignment,
			color = MaterialTheme.colorScheme.error
		)
		Spacer(Modifier.height(12.dp))
	}
}

@Composable
private fun QuestionScreen(
	question: QuestionDto,
	stepNumber: Int,
	totalQuestions: Int,
	onExit: () -> Unit,
	onSkipOptional: () -> Unit,
	onAnswer: (Any) -> Unit,
	onSkipInvalid: () -> Unit = {}
) {
	val configuration = LocalConfiguration.current
	val screenWidth = configuration.screenWidthDp.dp
	val landscape = configuration.screenWidthDp > configuration.screenHeightDp
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
			verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
		) {
				QuestionTopBar(
					stepNumber = stepNumber,
					totalQuestions = totalQuestions
				)
				QuestionTypeBadge(question.type)
				Text(
					question.text,
					fontSize = if (landscape) 30.sp else 24.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
				Text(
					stringResource(R.string.question_helper_subtitle),
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				when (question.type) {
					QuestionType.LIKERT_5 -> Likert5(
						isLandscape = landscape,
						maxWidth = screenWidth,
						onSelect = onAnswer
					)

					QuestionType.YES_NO -> YesNoRow(isLandscape = landscape, onAnswer = onAnswer)
					QuestionType.SINGLE -> SingleChoiceAnswer(
						options = question.options.orEmpty(),
						onAnswer = onAnswer,
						onSkipInvalid = onSkipInvalid
					)

					QuestionType.MULTI -> MultiChoiceAnswer(
						options = question.options.orEmpty(),
						onAnswer = onAnswer,
						onSkipInvalid = onSkipInvalid
					)

					QuestionType.TEXT -> TextAnswer(onAnswer)
					else -> Text(stringResource(R.string.question_type_not_implemented, question.type.name))
				}
				QuestionFooter(
					stepNumber = stepNumber,
					totalQuestions = totalQuestions,
					canSkip = !question.required,
					onExit = onExit,
					onSkip = onSkipOptional
				)
		}
	}
}

@Composable
private fun QuestionTopBar(
	stepNumber: Int,
	totalQuestions: Int
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Center
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			repeat(totalQuestions.coerceAtMost(5)) { idx ->
				val active = idx == (stepNumber - 1).coerceAtLeast(0)
				Box(
					modifier = Modifier
						.height(8.dp)
						.width(if (active) 28.dp else 8.dp)
						.background(
							color = if (active) TabletAccent else Color(0xFFD2D6DF),
							shape = MaterialTheme.shapes.extraLarge
						)
				)
			}
		}
	}
}

@Composable
private fun QuestionTypeBadge(type: QuestionType) {
	val label = when (type) {
		QuestionType.LIKERT_5 -> stringResource(R.string.question_type_likert_short)
		QuestionType.YES_NO -> stringResource(R.string.question_type_yesno_short)
		QuestionType.TEXT -> stringResource(R.string.question_type_text_short)
		QuestionType.SINGLE -> stringResource(R.string.question_type_single_short)
		QuestionType.MULTI -> stringResource(R.string.question_type_multi_short)
		else -> "—"
	}
	Surface(
		color = TabletAccentSoft,
		shape = MaterialTheme.shapes.extraLarge,
		tonalElevation = 0.dp
	) {
		Text(
			label,
			modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
			fontSize = 14.sp,
			fontWeight = FontWeight.SemiBold,
			color = TabletAccent
		)
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
	val faces = listOf("😡", "🙁", "😐", "🙂", "😄")
	if (!isLandscape) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			(1..5).forEach { score ->
				ElevatedButton(
					onClick = { onSelect(score) },
					modifier = Modifier
						.fillMaxWidth(0.92f)
						.align(Alignment.CenterHorizontally)
						.height(64.dp),
					shape = MaterialTheme.shapes.extraLarge
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.Center,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(faces[score - 1], fontSize = 28.sp)
						Spacer(Modifier.width(10.dp))
						Text("$score", fontSize = 22.sp, fontWeight = FontWeight.Bold)
					}
				}
			}
			Row(
				modifier = Modifier
					.fillMaxWidth(0.92f)
					.align(Alignment.CenterHorizontally),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(stringResource(R.string.question_scale_poor), color = Color(0xFFF07C74))
				Text(stringResource(R.string.question_scale_excellent), color = TabletAccent.copy(alpha = 0.7f))
			}
		}
	} else {
		FlowRow(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(
				space = 20.dp,
				alignment = Alignment.CenterHorizontally
			),
			verticalArrangement = Arrangement.spacedBy(16.dp),
			maxItemsInEachRow = 5
		) {
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
		) { Text(stringResource(R.string.answer_yes), fontSize = if (isLandscape) 28.sp else 24.sp) }
		ElevatedButton(
			onClick = { onAnswer(false) },
			modifier = Modifier.defaultMinSize(
				minWidth = if (isLandscape) 140.dp else 120.dp,
				minHeight = if (isLandscape) 64.dp else 56.dp
			)
		) { Text(stringResource(R.string.answer_no), fontSize = if (isLandscape) 28.sp else 24.sp) }
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
			label = { Text(stringResource(R.string.answer_your_response)) }
		)
		Spacer(Modifier.height(16.dp))
		Button(onClick = { onAnswer(txt) }) { Text(stringResource(R.string.action_continue)) }
	}
}

@Composable
private fun QuestionFooter(
	stepNumber: Int,
	totalQuestions: Int,
	canSkip: Boolean,
	onExit: () -> Unit,
	onSkip: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			stringResource(R.string.question_progress, stepNumber, totalQuestions),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
			TextButton(onClick = onExit) {
				Text(
					stringResource(R.string.question_exit),
					fontWeight = FontWeight.SemiBold,
					color = TabletAccent
				)
			}
			if (canSkip) {
				Surface(
					onClick = onSkip,
					shape = MaterialTheme.shapes.extraLarge,
					color = Color.Transparent,
					border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
						brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC5D5E5))
					)
				) {
					Row(
						modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(6.dp)
					) {
						Text(
							stringResource(R.string.question_skip),
							fontWeight = FontWeight.Bold,
							color = TabletAccent
						)
						Text(
							stringResource(R.string.question_skip_arrow),
							fontWeight = FontWeight.Bold,
							color = TabletAccent
						)
					}
				}
			}
		}
	}
}

@Composable
private fun SingleChoiceAnswer(
	options: List<QOptionDTO>,
	onAnswer: (String) -> Unit,
	onSkipInvalid: () -> Unit
) {
	if (options.isEmpty()) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text(
				stringResource(R.string.question_no_options),
				textAlign = TextAlign.Center
			)
			Button(onClick = onSkipInvalid) {
				Text(stringResource(R.string.action_continue))
			}
		}
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
private fun MultiChoiceAnswer(
	options: List<QOptionDTO>,
	onAnswer: (List<String>) -> Unit,
	onSkipInvalid: () -> Unit
) {
	if (options.isEmpty()) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text(
				stringResource(R.string.question_no_options),
				textAlign = TextAlign.Center
			)
			Button(onClick = onSkipInvalid) {
				Text(stringResource(R.string.action_continue))
			}
		}
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
			Text(stringResource(R.string.action_continue))
		}
	}
}

@Composable
private fun SubmitScreen(offlineMode: Boolean, onSubmit: (String?, Boolean?) -> Unit, message: String?) {
	var email by remember { mutableStateOf("") }
	var marketingOptIn by remember { mutableStateOf(false) }
	var emailError by remember { mutableStateOf<String?>(null) }
	val configuration = LocalConfiguration.current
	val landscape = configuration.screenWidthDp > configuration.screenHeightDp
	val emailRequiredErrorText = stringResource(R.string.submit_error_email_required)
	val emailInvalidErrorText = stringResource(R.string.submit_error_email_invalid)

	fun submitWithValidation(forceWithoutEmail: Boolean = false) {
		val trimmed = email.trim()
		if (forceWithoutEmail) {
			emailError = null
			onSubmit(null, false)
			return
		}
		if (trimmed.isBlank()) {
			if (marketingOptIn) {
				emailError = emailRequiredErrorText
			} else {
				emailError = null
				onSubmit(null, false)
			}
			return
		}
		val finalEmail = trimmed
		if (!isValidEmail(finalEmail)) {
			emailError = emailInvalidErrorText
			return
		}
		emailError = null
		onSubmit(finalEmail, marketingOptIn)
	}

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
						Text(stringResource(R.string.submit_title), fontSize = 26.sp, fontWeight = FontWeight.Bold)
						Spacer(Modifier.height(8.dp))
						Text(stringResource(R.string.submit_email_hint), fontSize = 14.sp)
						if (offlineMode) {
							Spacer(Modifier.height(8.dp))
							Text(
								stringResource(R.string.submit_offline_hint),
								fontSize = 13.sp,
								color = MaterialTheme.colorScheme.primary
							)
						}
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
				Text(
					stringResource(R.string.submit_title),
					fontSize = 24.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
				Spacer(Modifier.height(16.dp))
				if (offlineMode) {
					Text(
						stringResource(R.string.submit_offline_hint),
						fontSize = 13.sp,
						textAlign = TextAlign.Center,
						color = MaterialTheme.colorScheme.primary
					)
					Spacer(Modifier.height(12.dp))
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
					modifier = Modifier.fillMaxWidth(0.9f)
				)
			}
			if (message != null) {
				Spacer(Modifier.height(16.dp))
				Text(message, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
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
			label = { Text(stringResource(R.string.submit_email_optional)) },
			singleLine = true,
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
		)
		Spacer(Modifier.height(8.dp))
		Text(
			stringResource(R.string.submit_email_hint),
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
			Text(stringResource(R.string.submit_marketing_opt_in))
		}
		if (emailError != null) {
			Spacer(Modifier.height(8.dp))
			Text(emailError, color = MaterialTheme.colorScheme.error)
		}
		Spacer(Modifier.height(16.dp))
		Button(
			onClick = onSubmit,
			modifier = Modifier.fillMaxWidth()
		) { Text(stringResource(R.string.submit_action_send)) }
		Spacer(Modifier.height(8.dp))
		TextButton(onClick = onSubmitWithoutEmail) {
			Text(stringResource(R.string.submit_action_send_without_email))
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
				hasQuestions = true,
				offlineMode = false,
				pendingSyncCount = 0,
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
				stepNumber = 1,
				totalQuestions = 4,
				onExit = {},
				onSkipOptional = {},
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
				stepNumber = 1,
				totalQuestions = 4,
				onExit = {},
				onSkipOptional = {},
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
				stepNumber = 1,
				totalQuestions = 4,
				onExit = {},
				onSkipOptional = {},
				onAnswer = {}
			)
		}
	}
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Submit")
@Composable
private fun PreviewSubmit() {
	MaterialTheme { Surface { SubmitScreen(offlineMode = false, onSubmit = { _, _ -> }, message = "¡Gracias!") } }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600, name = "Submit Offline")
@Composable
private fun PreviewSubmitOffline() {
	MaterialTheme { Surface { SubmitScreen(offlineMode = true, onSubmit = { _, _ -> }, message = null) } }
}
