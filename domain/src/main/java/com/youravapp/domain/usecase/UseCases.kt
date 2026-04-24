package com.youravapp.domain.usecase

import com.youravapp.domain.model.InstalledApp
import com.youravapp.domain.model.ThreatAction
import com.youravapp.domain.model.ThreatDetection
import com.youravapp.domain.model.ThreatPolicy
import com.youravapp.domain.model.ViolationAction
import com.youravapp.domain.repository.IAppRepository
import com.youravapp.domain.repository.IPolicyRepository
import com.youravapp.domain.repository.IQuarantineRepository
import com.youravapp.domain.repository.IScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class ScanFilesUseCase(
    private val scanRepository: IScanRepository,
    private val quarantineRepository: IQuarantineRepository,
    private val appRepository: IAppRepository
) {
    suspend operator fun invoke(onDecision: suspend (ThreatDetection) -> ThreatAction): Result<Unit> {
        val whitelist = appRepository.whitelistPackages().first()
        return scanRepository.scanAllFiles(excludedPaths = whitelist) { detection ->
            when (val action = onDecision(detection)) {
                ThreatAction.QUARANTINE -> quarantineRepository.quarantine(detection.filePath)
                ThreatAction.DELETE -> quarantineRepository.delete(detection.filePath)
                ThreatAction.IGNORE -> quarantineRepository.ignore(detection)
            }
            action
        }
    }
}

class GetInstalledAppsUseCase(private val appRepository: IAppRepository) {
    suspend operator fun invoke(): List<InstalledApp> = appRepository.getInstalledApps()
}

class ToggleComponentUseCase(private val appRepository: IAppRepository) {
    suspend operator fun invoke(packageName: String, componentName: String, enabled: Boolean): Result<Unit> {
        return appRepository.setComponentEnabled(packageName, componentName, enabled)
    }
}

class EvaluatePolicyUseCase(private val repository: IPolicyRepository) {
    suspend fun evaluate(app: InstalledApp): List<Pair<ThreatPolicy, ViolationAction>> {
        return repository.getPolicies().filter { it.enabled }.mapNotNull { policy ->
            val violation = when {
                policy.matcher.disallowDebugCertificatesAtBoot && app.signatureSha256.contains("DEBUG", true) -> true
                policy.matcher.requiresUserApprovalForAudioInternet &&
                    app.permissions.contains("android.permission.RECORD_AUDIO") &&
                    app.permissions.contains("android.permission.INTERNET") -> true
                else -> false
            }
            if (violation) policy to policy.action else null
        }
    }
}

class EvaluateBehaviorUseCase {
    fun ratRiskScore(app: InstalledApp): Int {
        var score = 0
        if (app.permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) score += 25
        if (app.permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) score += 15
        if (app.permissions.contains("android.permission.RECORD_AUDIO")) score += 20
        if (app.permissions.contains("android.permission.CAMERA")) score += 10
        if (app.permissions.contains("android.permission.INTERNET")) score += 15
        if (app.components.any { it.type.name == "SERVICE" && it.exported }) score += 15
        return score.coerceAtMost(100)
    }
}

class MonitorProcessesUseCase(
    private val appRepository: IAppRepository,
    private val installedApps: GetInstalledAppsUseCase,
    private val evaluatePolicyUseCase: EvaluatePolicyUseCase
) {
    operator fun invoke(): Flow<Pair<String, ViolationAction>> = flow {
        val apps = installedApps().associateBy { it.packageName }
        appRepository.monitorProcessStarts().collectLatest { pkg ->
            val app = apps[pkg] ?: return@collectLatest
            evaluatePolicyUseCase.evaluate(app).firstOrNull()?.let { emit(pkg to it.second) }
        }
    }
}
