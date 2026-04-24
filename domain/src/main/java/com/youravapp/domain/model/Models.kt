package com.youravapp.domain.model

import java.time.Instant

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val permissions: List<String>,
    val signatureSha256: String,
    val lastUpdateTime: Long,
    val components: List<AppComponent>
)

data class AppComponent(
    val name: String,
    val type: ComponentType,
    val exported: Boolean,
    val enabled: Boolean,
    val autostartActions: List<String> = emptyList()
)

enum class ComponentType { ACTIVITY, SERVICE, RECEIVER, PROVIDER }

data class ScanProgress(
    val scanned: Int,
    val total: Int,
    val currentPath: String,
    val startedAt: Instant
)

enum class ThreatCategory { MALWARE, RANSOMWARE, RAT, POLICY_VIOLATION, SUSPICIOUS_ARCHIVE }

data class ThreatDetection(
    val filePath: String,
    val threatName: String,
    val category: ThreatCategory = ThreatCategory.MALWARE,
    val confidence: Int = 60,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ThreatAction { QUARANTINE, IGNORE, DELETE }

data class ThreatPolicy(
    val id: String,
    val description: String,
    val enabled: Boolean,
    val matcher: PolicyMatcher,
    val action: ViolationAction
)

data class PolicyMatcher(
    val requiresUserApprovalForAudioInternet: Boolean = false,
    val disallowDebugCertificatesAtBoot: Boolean = false,
    val trustedDomains: List<String> = emptyList(),
    val controlledFolders: List<String> = emptyList()
)

enum class ViolationAction { BLOCK_AND_PROMPT, FORCE_STOP, REVOKE_PERMISSION, QUARANTINE_APK }

data class SandboxSession(
    val packageName: String,
    val startedAt: Long,
    val durationMs: Long,
    val events: List<String> = emptyList(),
    val suspiciousCount: Int = 0,
    val completed: Boolean = false
)

data class SandboxAnalysis(
    val packageName: String,
    val score: Int,
    val verdict: SandboxVerdict,
    val findings: List<String>
)

enum class SandboxVerdict { SAFE, REVIEW, BLOCK }
