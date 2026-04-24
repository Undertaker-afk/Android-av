package com.youravapp.data.shizuku

import android.content.pm.PackageManager
import dev.rikka.shizuku.Shizuku

class ShizukuManager {
    fun isAvailable(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission() {
        if (!hasPermission()) Shizuku.requestPermission(0)
    }

    fun validateSystemAccess(): Boolean {
        return isAvailable() && hasPermission()
    }

    fun setupGuide(): List<String> = listOf(
        "Install Shizuku from Play Store or GitHub.",
        "Enable wireless debugging in Android developer options.",
        "Pair device and start Shizuku service.",
        "Return to Trinity-AV and grant Shizuku permission."
    )
}
