plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.serialization")
	id("org.jetbrains.kotlin.plugin.compose")
	id("com.google.devtools.ksp")
	id("com.google.dagger.hilt.android")
	id("com.google.gms.google-services")
}

android {
	namespace = "com.mr.restaurant.survey.admin"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.mr.restaurant.admin"
		minSdk = 26
		targetSdk = 34
		versionCode = 1
		versionName = "0.1"
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		jvmTarget = "17"
	}

	buildFeatures {
		compose = true
	}

	packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
	implementation(project(":core"))

	implementation(platform("androidx.compose:compose-bom:2024.09.02"))
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.navigation:navigation-compose:2.9.7")
	implementation("androidx.compose.runtime:runtime-saveable:1.10.3")
	implementation("androidx.navigation:navigation-runtime-ktx:2.9.7")
	implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
	debugImplementation("androidx.compose.ui:ui-tooling")
	implementation("androidx.compose.material3:material3:1.3.0")
	implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
	implementation("com.google.firebase:firebase-messaging-ktx")
	implementation("com.google.firebase:firebase-analytics-ktx")

	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

	implementation("com.google.android.material:material:1.12.0")
	implementation("androidx.appcompat:appcompat:1.7.0")
	implementation("com.google.zxing:core:3.5.3")
	implementation("com.google.dagger:hilt-android:2.51.1")
	ksp("com.google.dagger:hilt-android-compiler:2.51.1")

	testImplementation("junit:junit:4.13.2")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
	androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	androidTestImplementation("androidx.test.ext:junit:1.2.1")
	androidTestImplementation("androidx.test:runner:1.6.2")
	debugImplementation("androidx.compose.ui:ui-test-manifest")

}
