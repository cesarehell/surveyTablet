package com.mr.restaurant.survey.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mr.restaurant.survey.data.SurveyRepository
import com.mr.restaurant.survey.net.SurveyTemplateDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val error: String? = null,
    val instanceId: String? = null,
    val template: SurveyTemplateDTO? = null,
    val stepIndex: Int = -1,
    val celebrating: Boolean = false
)

class AppViewModel(
    private val repo: SurveyRepository,
    private val tenant: String,
    private val templateId: String?,
    private val locationId: String? = null,
    private val table: String = "A7",
    private val waiter: String = "ERIKA"
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init { bootstrap() }

    private fun bootstrap() = viewModelScope.launch {
        runCatching {
            _state.value = _state.value.copy(loading = true, error = null)

                val t = if (templateId != null) {
                      repo.loadTemplateWithOptions(templateId)
                   } else {
                       repo.pickCurrentTemplate(tenant)
                }
               val tplId = t.id

            val startResp = repo.startSurvey(
                tenant = tenant,
                templateId = tplId,
                locationId = locationId,
                table = table,
                waiter = waiter
            )

            _state.value = UiState(
                loading = false,
                instanceId = startResp.instanceId,
                template = t,
                stepIndex = -1
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = e.message ?: "Error de red")
        }
    }

    fun next() { _state.value = _state.value.copy(stepIndex = _state.value.stepIndex + 1) }

    fun postAnswer(qid: String, value: Any) = viewModelScope.launch {
        val inst = _state.value.instanceId ?: return@launch
        runCatching { repo.postAnswer(inst, qid, value) }
            .onFailure { _state.value = _state.value.copy(error = it.message) }
    }

    fun submitAndRestart() = viewModelScope.launch {
                val inst = _state.value.instanceId ?: return@launch
               runCatching { repo.submit(inst) }
                    .onSuccess {
                        _state.value = _state.value.copy(celebrating = true)
                        kotlinx.coroutines.delay(1200)

                           val tplId = _state.value.template?.id
                               ?: error("Template no cargado")
                          val startResp = repo.startSurvey(
                                  tenant = tenant,
                                   templateId = tplId,
                                   locationId = locationId,
                                    table = table,
                                    waiter = waiter
                                        )
                            _state.value = _state.value.copy(
                                   instanceId = startResp.instanceId,
                                   stepIndex = -1,
                                error = null,
                                celebrating = false
                                        )
                       }
                    .onFailure { e ->
                            _state.value = _state.value.copy(error = e.message ?: "Error al enviar")
                       }
            }
}
