package com.mr.restaurant.survey.core.net

import kotlinx.coroutines.CancellationException

sealed class ApiResult<out T> {
	data class Ok<T>(val value: T) : ApiResult<T>()
	data class Err(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>()
}

suspend inline fun <T> safeCall(
	crossinline block: suspend () -> T
): ApiResult<T> {
	return try {
		ApiResult.Ok(block())
	} catch (c: CancellationException) {
		throw c
	} catch (t: Throwable) {
		ApiResult.Err(ApiErrorMapper.message(t), t)
	}
}
