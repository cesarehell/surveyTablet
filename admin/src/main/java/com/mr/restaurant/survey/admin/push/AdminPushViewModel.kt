package com.mr.restaurant.survey.admin.push

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AdminPushViewModel @Inject constructor(
	private val registrar: AdminPushRegistrar
) : ViewModel() {
	fun bindTenant(tenantId: String) {
		registrar.bindTenant(tenantId)
	}
}
