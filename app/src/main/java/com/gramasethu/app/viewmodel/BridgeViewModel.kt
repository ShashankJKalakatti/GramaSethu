package com.gramasethu.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.gramasethu.app.model.Bridge
import com.gramasethu.app.model.BridgeReport
import com.gramasethu.app.model.BridgeStatus
import com.gramasethu.app.repository.BridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BridgeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BridgeRepository.get()

    private val filterFlow = MutableStateFlow<BridgeStatus?>(null)
    private val queryFlow  = MutableStateFlow("")

    val allBridges: LiveData<List<Bridge>> = repo.bridgesFlow.asLiveData()

    val filteredBridges: LiveData<List<Bridge>> =
        combine(repo.bridgesFlow, filterFlow, queryFlow) { list, f, q ->
            list.filter { b ->
                (f == null || b.effectiveStatus == f) &&
                (q.isBlank() || b.name.contains(q, true) ||
                 b.village.contains(q, true) || b.district.contains(q, true))
            }
        }.asLiveData()

    private val _reportState = MutableLiveData<ReportState>(ReportState.Idle)
    val reportState: LiveData<ReportState> = _reportState

    init { viewModelScope.launch { repo.seedIfEmpty() } }

    fun setFilter(s: BridgeStatus?) { filterFlow.value = s }
    fun setQuery(q: String)         { queryFlow.value  = q }

    fun getBridge(id: String) = allBridges.value?.find { it.id == id }

    fun submitReport(bridgeId: String, status: String, name: String,
                     notes: String, wl: Double, loc: GeoPoint?) {
        _reportState.value = ReportState.Loading
        val report = BridgeReport(
            bridgeId           = bridgeId,
            reportedStatus     = status,
            reporterName       = name.ifBlank { "Anonymous Grama-Kavalu" },
            notes              = notes,
            reporterLocation   = loc ?: GeoPoint(0.0, 0.0),
            waterLevelObserved = wl,
            reportedAt         = Timestamp.now()
        )
        viewModelScope.launch {
            repo.submitReport(report)
                .onSuccess { _reportState.value = ReportState.Success }
                .onFailure { _reportState.value = ReportState.Error(it.message ?: "Error") }
        }
    }

    fun resetReport() { _reportState.value = ReportState.Idle }

    sealed class ReportState {
        object Idle    : ReportState()
        object Loading : ReportState()
        object Success : ReportState()
        data class Error(val msg: String) : ReportState()
    }
}
