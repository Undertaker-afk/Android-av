package com.youravapp.data.jni

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ClamAvNativeBridge {
    init {
        runCatching { System.loadLibrary("clamav_jni") }
            .onFailure { throw IllegalStateException("Failed to load native scanner library", it) }
    }

    external fun initClamAV(dbPath: String): Int
    external fun scanFile(filePath: String): String
    external fun scanBytes(name: String, content: ByteArray): String
    external fun updateDatabase(url: String): Boolean
    external fun startSandbox(packageName: String): Boolean
    external fun stopSandbox(packageName: String): Boolean
    external fun analyzeSandbox(packageName: String): String

    suspend fun scan(filePath: String): String = withContext(Dispatchers.IO) { scanFile(filePath) }
}
