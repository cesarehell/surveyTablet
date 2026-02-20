package com.mr.restaurant.survey.core.net

object ApiConfig {

	@Volatile
	var baseUrl: String = "http://192.168.100.69:3040/"
		set(value) {
			field = value.trim().let { if (it.endsWith("/")) it else "$it/" }
		}
}
