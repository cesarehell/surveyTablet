plugins {
	id("com.android.library")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.serialization")
}

android {
	namespace = "com.mr.restaurant.survey.core"
	compileSdk = 34

	defaultConfig {
		minSdk = 26
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}
}

dependencies {
	api("com.squareup.retrofit2:retrofit:2.11.0")
	api("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
	api("com.squareup.okhttp3:okhttp:4.12.0")
	api("com.squareup.okhttp3:logging-interceptor:4.12.0")

	api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

