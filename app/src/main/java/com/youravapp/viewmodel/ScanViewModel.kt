package com.youravapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youravapp.domain.model.ScanProgress
import com.youravapp.domain.model.ThreatAction
import com.youravapp.domain.model.ThreatDetection
import com.youravapp.domain.usecase.ScanFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanFilesUseCase: ScanFilesUseCase
) : ViewModel() {
    private val _progress = MutableStateFlow(ScanProgress(0, 0, "", java.time.Instant.now()))
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val _pendingDetection = MutableStateFlow<ThreatDetection?>(null)
    val pendingDetection = _pendingDetection.asStateFlow()

    private var decisionContinuation: ((ThreatAction) -> Unit)? = null

    fun startScan() {
        viewModelScope.launch {
            scanFilesUseCase { detection ->
                suspendCancellableCoroutine { cont ->
                    _pendingDetection.value = detection
                    decisionContinuation = { action ->
                        _pendingDetection.value = null
                        cont.resume(action)
                    }
                }
            }
        }
    }

    fun onDetectionDecision(action: ThreatAction) {
        decisionContinuation?.invoke(action)
        decisionContinuation = null
    }
}
