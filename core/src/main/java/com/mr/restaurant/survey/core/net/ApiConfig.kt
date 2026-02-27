package com.mr.restaurant.survey.core.net

import okhttp3.logging.HttpLoggingInterceptor

object ApiConfig {

	@Volatile
	var baseUrl: String = "http://192.168.100.69:3040/"
		set(value) {
			field = value.trim().let { if (it.endsWith("/")) it else "$it/" }
		}

	@Volatile
	var httpLogLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE
}
