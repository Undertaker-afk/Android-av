package com.youravapp.data.repository

import android.content.Context
import com.youravapp.data.datastore.SettingsStore
import com.youravapp.data.jni.ClamAvNativeBridge
import com.youravapp.data.shizuku.SystemPackageProxy
import com.youravapp.domain.model.InstalledApp
import com.youravapp.domain.model.ScanProgress
import com.youravapp.domain.model.ThreatAction
import com.youravapp.domain.model.ThreatCategory
import com.youravapp.domain.model.ThreatDetection
import com.youravapp.domain.model.ThreatPolicy
import com.youravapp.domain.repository.IAppRepository
import com.youravapp.domain.repository.IPolicyRepository
import com.youravapp.domain.repository.IQuarantineRepository
import com.youravapp.domain.repository.IScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class ScanRepositoryImpl(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val maxFileSizeMb: Long = 200
) : IScanRepository {
    private val mutableProgress = MutableStateFlow(ScanProgress(0, 0, "", Instant.now()))
    override val progress = mutableProgress.asStateFlow()

    private val controlledFolders = listOf("/sdcard/Documents", "/sdcard/DCIM", "/sdcard/Download")

    override suspend fun initEngine(dbPath: String): Boolean = withContext(Dispatchers.IO) {
        ClamAvNativeBridge.initClamAV(dbPath) == 0
    }

    override suspend fun scanAllFiles(
        excludedPaths: Set<String>,
        onDetection: suspend (ThreatDetection) -> ThreatAction
    ): Result<Unit> = runCatching {
        val excluded = excludedPaths + settingsStore.excludedPathsFlow.asStateSet()
        val roots = listOfNotNull(context.getExternalFilesDir(null)?.parentFile, File("/sdcard"))
            .filterNot { root -> excluded.any { root.absolutePath.startsWith(it) } }

        val files = roots.flatMap { root ->
            root.walkTopDown().filter { file ->
                file.isFile && file.canRead() && excluded.none { ex -> file.absolutePath.startsWith(ex) }
            }.toList()
        }

        mutableProgress.value = ScanProgress(0, files.size, "", Instant.now())
        val ransomwareSignals = mutableMapOf<String, Int>()

        files.forEachIndexed { index, file ->
            mutableProgress.value = mutableProgress.value.copy(scanned = index + 1, currentPath = file.absolutePath)
            if (file.length() > maxFileSizeMb * 1024 * 1024) return@forEachIndexed

            val nativeResult = ClamAvNativeBridge.scan(file.absolutePath)
            nativeResult.toDetection(file.absolutePath)?.let { onDetection(it) }

            if (file.extension.lowercase() in setOf("zip", "apk", "jar")) {
                scanArchive(file).forEach { onDetection(it) }
            }

            if (isRansomwareArtifact(file)) {
                val folder = controlledFolders.firstOrNull { file.absolutePath.startsWith(it) } ?: return@forEachIndexed
                ransomwareSignals[folder] = (ransomwareSignals[folder] ?: 0) + 1
                if ((ransomwareSignals[folder] ?: 0) >= 5) {
                    onDetection(
                        ThreatDetection(
                            filePath = folder,
                            threatName = "Ransomware-Mass-Encryption-Behavior",
                            category = ThreatCategory.RANSOMWARE,
                            confidence = 90
                        )
                    )
                }
            }
        }
    }

    private fun isRansomwareArtifact(file: File): Boolean {
        val encryptedExt = setOf("locked", "crypt", "enc", "encrypted", "ryk")
        val notes = setOf("how_to_decrypt", "readme_decrypt", "recover_files")
        return file.extension.lowercase() in encryptedExt || notes.any { file.name.lowercase().contains(it) }
    }

    private suspend fun scanArchive(archive: File): List<ThreatDetection> = withContext(Dispatchers.IO) {
        val detections = mutableListOf<ThreatDetection>()
        runCatching {
            ZipInputStream(FileInputStream(archive)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zis.readBytes().take(2 * 1024 * 1024).toByteArray()
                        val res = ClamAvNativeBridge.scanBytes(entry.name, bytes)
                        res.toDetection("${archive.absolutePath}!/${entry.name}")?.let {
                            detections += it.copy(category = ThreatCategory.SUSPICIOUS_ARCHIVE)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        detections
    }

    private fun String.toDetection(path: String): ThreatDetection? {
        if (!startsWith("CL_VIRUS:")) return null
        val name = substringAfter(':').ifBlank { "Unknown" }
        val category = when {
            name.contains("ransom", true) || name.contains("lock", true) -> ThreatCategory.RANSOMWARE
            name.contains("rat", true) || name.contains("meterpreter", true) -> ThreatCategory.RAT
            else -> ThreatCategory.MALWARE
        }
        return ThreatDetection(path, name, category = category, confidence = 85)
    }
}

class AppRepositoryImpl(
    private val proxy: SystemPackageProxy,
    private val settingsStore: SettingsStore
) : IAppRepository {
    private val processEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)

    override suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) { proxy.getInstalledApps() }

    override suspend fun setComponentEnabled(packageName: String, componentName: String, enabled: Boolean): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { proxy.setComponentEnabled(packageName, componentName, enabled) }
    }

    override suspend fun addToWhitelist(packageName: String): Result<Unit> = runCatching {
        settingsStore.addWhitelistPackage(packageName)
    }

    override suspend fun removeFromWhitelist(packageName: String): Result<Unit> = runCatching {
        settingsStore.removeWhitelistPackage(packageName)
    }

    override fun whitelistPackages(): Flow<Set<String>> = settingsStore.whitelistPackagesFlow

    override fun monitorProcessStarts(): Flow<String> = processEvents.asSharedFlow()

    suspend fun emitProcessStart(packageName: String) {
        processEvents.emit(packageName)
    }
}

class QuarantineRepositoryImpl(private val context: Context) : IQuarantineRepository {
    private val quarantineDir by lazy { File(context.filesDir, "quarantine").apply { mkdirs() } }
    private val key: SecretKey by lazy {
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    override suspend fun quarantine(path: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val src = File(path)
            val dest = File(quarantineDir, src.name + ".qav")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(src.readBytes())
            dest.writeBytes(encrypted)
            src.delete()
            dest.absolutePath
        }
    }

    override suspend fun delete(path: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { File(path).delete() }
    }

    override suspend fun ignore(detection: ThreatDetection): Result<Unit> = Result.success(Unit)
}

class PolicyRepositoryImpl : IPolicyRepository {
    override suspend fun getPolicies(): List<ThreatPolicy> = listOf(
        ThreatPolicy(
            id = "debug-cert-boot",
            description = "Apps with debug certificates cannot autostart at boot.",
            enabled = true,
            matcher = com.youravapp.domain.model.PolicyMatcher(disallowDebugCertificatesAtBoot = true),
            action = com.youravapp.domain.model.ViolationAction.BLOCK_AND_PROMPT
        ),
        ThreatPolicy(
            id = "audio-internet",
            description = "Apps with RECORD_AUDIO + INTERNET require user approval.",
            enabled = true,
            matcher = com.youravapp.domain.model.PolicyMatcher(requiresUserApprovalForAudioInternet = true),
            action = com.youravapp.domain.model.ViolationAction.BLOCK_AND_PROMPT
        ),
        ThreatPolicy(
            id = "controlled-folder-access",
            description = "Untrusted apps cannot modify controlled folders without user confirmation.",
            enabled = true,
            matcher = com.youravapp.domain.model.PolicyMatcher(controlledFolders = listOf("/sdcard/Documents", "/sdcard/DCIM", "/sdcard/Download")),
            action = com.youravapp.domain.model.ViolationAction.BLOCK_AND_PROMPT
        )
    )
}

private suspend fun Flow<Set<String>>.asStateSet(): Set<String> = first()
