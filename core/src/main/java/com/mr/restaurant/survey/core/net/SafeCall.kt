package com.mr.restaurant.survey.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ApiResult<out T> {
	data class Ok<T>(val value: T) : ApiResult<T>()
	data class Err(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>()
}

suspend inline fun <T> safeCall(
	crossinline block: suspend () -> T
): ApiResult<T> = withContext(Dispatchers.IO) {
	try {
		ApiResult.Ok(block())
	} catch (t: Throwable) {
		ApiResult.Err(ApiErrorMapper.message(t), t)
	}
}