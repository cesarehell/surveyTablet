package com.mr.restaurant.survey.core.net

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiFactory {

	private val json = Json {
		ignoreUnknownKeys = true
		explicitNulls = false
	}

	private val client: OkHttpClient by lazy {
		val logging = HttpLoggingInterceptor().apply {
			level = ApiConfig.httpLogLevel
		}
		OkHttpClient.Builder()
			.addInterceptor(logging)
			.build()
	}

	fun retrofit(baseUrl: String): Retrofit =
		Retrofit.Builder()
			.baseUrl(baseUrl)
			.client(client)
			.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
			.build()

	fun retrofit(): Retrofit = retrofit(ApiConfig.baseUrl)

}
