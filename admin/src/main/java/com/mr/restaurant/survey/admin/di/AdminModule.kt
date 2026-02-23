package com.mr.restaurant.survey.admin.di

import com.mr.restaurant.survey.core.alert.api.AlertApi
import com.mr.restaurant.survey.core.device.api.DeviceApi
import com.mr.restaurant.survey.core.location.api.LocationApi
import com.mr.restaurant.survey.core.metrics.api.MetricsApi
import com.mr.restaurant.survey.core.net.ApiFactory
import com.mr.restaurant.survey.core.threshold.api.ThresholdApi
import com.mr.restaurant.survey.core.template.api.TemplateApi
import com.mr.restaurant.survey.core.tenant.TenantRepository
import com.mr.restaurant.survey.core.tenant.api.TenantApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdminModule {
	@Provides
	@Singleton
	fun provideRetrofit(): Retrofit = ApiFactory.retrofit()

	@Provides
	@Singleton
	fun provideTenantApi(retrofit: Retrofit): TenantApi =
		retrofit.create(TenantApi::class.java)

	@Provides
	@Singleton
	fun provideLocationApi(retrofit: Retrofit): LocationApi =
		retrofit.create(LocationApi::class.java)

	@Provides
	@Singleton
	fun provideTemplateApi(retrofit: Retrofit): TemplateApi =
		retrofit.create(TemplateApi::class.java)

	@Provides
	@Singleton
	fun provideThresholdApi(retrofit: Retrofit): ThresholdApi =
		retrofit.create(ThresholdApi::class.java)

	@Provides
	@Singleton
	fun provideAlertApi(retrofit: Retrofit): AlertApi =
		retrofit.create(AlertApi::class.java)

	@Provides
	@Singleton
	fun provideDeviceApi(retrofit: Retrofit): DeviceApi =
		retrofit.create(DeviceApi::class.java)

	@Provides
	@Singleton
	fun provideMetricsApi(retrofit: Retrofit): MetricsApi =
		retrofit.create(MetricsApi::class.java)

	@Provides
	@Singleton
	fun provideTenantRepository(api: TenantApi): TenantRepository = TenantRepository(api)
}
