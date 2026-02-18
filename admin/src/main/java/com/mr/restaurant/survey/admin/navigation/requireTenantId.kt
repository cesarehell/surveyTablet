package com.mr.restaurant.survey.admin.navigation

import android.os.Bundle

fun Bundle.requireTenantId(): String =
	requireNotNull(getString(Routes.TENANT_ID_ARG))
