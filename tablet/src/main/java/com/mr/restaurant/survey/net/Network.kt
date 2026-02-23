package com.mr.restaurant.survey.net

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mr.restaurant.survey.net.api.SurveyApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create

object Network {
	fun createApi(baseUrl: String): SurveyApi {
		val json = Json {
			encodeDefaults = true
			ignoreUnknownKeys = true
			explicitNulls = false
			isLenient = true
		}
		val client = OkHttpClient.Builder()
			.addInterceptor(HttpLoggingInterceptor().apply {
				level = HttpLoggingInterceptor.Level.BODY
			})
			.build()

		val contentType = "application/json".toMediaType()

		val retrofit = Retrofit.Builder()
			.baseUrl(baseUrl.ensureEndsWithSlash())
			.addConverterFactory(json.asConverterFactory(contentType))
			.client(client)
			.build()

		return retrofit.create()
	}

	private fun String.ensureEndsWithSlash(): String =
		if (endsWith("/")) this else "$this/"
}
