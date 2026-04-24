package com.youravapp.domain.repository

import com.youravapp.domain.model.InstalledApp
import com.youravapp.domain.model.ScanProgress
import com.youravapp.domain.model.ThreatAction
import com.youravapp.domain.model.ThreatDetection
import com.youravapp.domain.model.ThreatPolicy
import kotlinx.coroutines.flow.Flow

interface IScanRepository {
    suspend fun initEngine(dbPath: String): Boolean
    suspend fun scanAllFiles(
        excludedPaths: Set<String> = emptySet(),
        onDetection: suspend (ThreatDetection) -> ThreatAction
    ): Result<Unit>
    val progress: Flow<ScanProgress>
}

interface IAppRepository {
    suspend fun getInstalledApps(): List<InstalledApp>
    suspend fun setComponentEnabled(packageName: String, componentName: String, enabled: Boolean): Result<Unit>
    suspend fun addToWhitelist(packageName: String): Result<Unit>
    suspend fun removeFromWhitelist(packageName: String): Result<Unit>
    fun whitelistPackages(): Flow<Set<String>>
    fun monitorProcessStarts(): Flow<String>
}

interface IQuarantineRepository {
    suspend fun quarantine(path: String): Result<String>
    suspend fun delete(path: String): Result<Unit>
    suspend fun ignore(detection: ThreatDetection): Result<Unit>
}

interface IPolicyRepository {
    suspend fun getPolicies(): List<ThreatPolicy>
}
