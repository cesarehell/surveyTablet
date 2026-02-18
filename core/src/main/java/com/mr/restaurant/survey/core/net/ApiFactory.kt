package com.mr.restaurant.survey.core.net

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiFactory {
	fun retrofit(baseUrl: String): Retrofit {
		val logging = HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY
		}

		val client = OkHttpClient.Builder()
			.addInterceptor(logging)
			.build()

		val json = Json {
			ignoreUnknownKeys = true
			explicitNulls = false
		}

		return Retrofit.Builder()
			.baseUrl(baseUrl)
			.client(client)
			.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
			.build()
	}
}
