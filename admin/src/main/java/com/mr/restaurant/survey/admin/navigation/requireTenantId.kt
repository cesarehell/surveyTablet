package com.mr.restaurant.survey.admin.navigation

import android.net.Uri
import android.os.Bundle

fun Bundle.requireTenantId(): String =
	Uri.decode(requireNotNull(getString(Routes.TENANT_ID_ARG)))

fun Bundle.requireTemplateId(): String =
	Uri.decode(requireNotNull(getString(Routes.TEMPLATE_ID_ARG)))
