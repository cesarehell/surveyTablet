package com.mr.restaurant.survey.admin.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mr.restaurant.survey.admin.R

@Composable
fun adminQuestionTypeLabel(type: String): String = when (type.uppercase()) {
	"LIKERT_5" -> stringResource(R.string.question_type_likert_5)
	"YESNO" -> stringResource(R.string.question_type_yesno)
	"TEXT" -> stringResource(R.string.question_type_text)
	"SINGLE" -> stringResource(R.string.question_type_single)
	"MULTI" -> stringResource(R.string.question_type_multi)
	else -> type
}

@Composable
fun adminThresholdTypeLabel(type: String): String = when (type.uppercase()) {
	"NEGATIVE" -> stringResource(R.string.thresholds_type_negative)
	"LT" -> stringResource(R.string.thresholds_type_lt)
	"LE" -> stringResource(R.string.thresholds_type_le)
	"EQ" -> stringResource(R.string.thresholds_type_eq)
	"GE" -> stringResource(R.string.thresholds_type_ge)
	else -> type
}

@Composable
fun adminCouponTriggerTypeLabel(type: String): String = when (type.uppercase()) {
	"NEGATIVE" -> stringResource(R.string.coupon_trigger_negative)
	"VISITS_GE" -> stringResource(R.string.coupon_trigger_visits_ge)
	"LE" -> stringResource(R.string.thresholds_type_le)
	"LT" -> stringResource(R.string.thresholds_type_lt)
	"EQ" -> stringResource(R.string.thresholds_type_eq)
	else -> type
}

@Composable
fun adminCouponOfferTypeLabel(type: String): String = when (type.uppercase()) {
	"PERCENT" -> stringResource(R.string.coupon_offer_percent)
	"AMOUNT" -> stringResource(R.string.coupon_offer_amount)
	"PRODUCT" -> stringResource(R.string.coupon_offer_product)
	else -> type
}
