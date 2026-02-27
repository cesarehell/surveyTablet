package com.mr.restaurant.survey.net

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mr.restaurant.survey.BuildConfig
import com.mr.restaurant.survey.net.api.SurveyApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create
import java.util.concurrent.ConcurrentHashMap

object Network {
	private val apiCache = ConcurrentHashMap<String, SurveyApi>()
	private val contentType = "application/json".toMediaType()

	fun createApi(baseUrl: String): SurveyApi {
		val normalized = baseUrl.ensureEndsWithSlash()
		apiCache[normalized]?.let { return it }

		val json = Json {
			encodeDefaults = true
			ignoreUnknownKeys = true
			explicitNulls = false
			isLenient = true
		}
		val client = OkHttpClient.Builder()
			.addInterceptor(HttpLoggingInterceptor().apply {
				level = if (BuildConfig.DEBUG) {
					HttpLoggingInterceptor.Level.BASIC
				} else {
					HttpLoggingInterceptor.Level.NONE
				}
			})
			.build()

		val retrofit = Retrofit.Builder()
			.baseUrl(normalized)
			.addConverterFactory(json.asConverterFactory(contentType))
			.client(client)
			.build()

		return retrofit.create<SurveyApi>().also { apiCache[normalized] = it }
	}

	private fun String.ensureEndsWithSlash(): String =
		if (endsWith("/")) this else "$this/"
}
