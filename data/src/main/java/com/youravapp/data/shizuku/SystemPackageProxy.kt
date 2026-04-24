package com.youravapp.data.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.youravapp.domain.model.AppComponent
import com.youravapp.domain.model.ComponentType
import com.youravapp.domain.model.InstalledApp

class SystemPackageProxy(private val context: Context) {
    private val pm = context.packageManager

    fun getInstalledApps(): List<InstalledApp> {
        return pm.getInstalledPackages(
            PackageManager.GET_PERMISSIONS or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_PROVIDERS
        ).map { pkg ->
            val signatures = pkg.signingInfo?.apkContentsSigners?.joinToString { it.toCharsString() }.orEmpty()
            InstalledApp(
                packageName = pkg.packageName,
                appName = pm.getApplicationLabel(pkg.applicationInfo).toString(),
                isSystem = pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0,
                permissions = pkg.requestedPermissions?.toList().orEmpty(),
                signatureSha256 = signatures.take(64),
                lastUpdateTime = pkg.lastUpdateTime,
                components = buildComponents(pkg.packageName, pkg.activities.orEmpty(), pkg.services.orEmpty(), pkg.receivers.orEmpty(), pkg.providers.orEmpty())
            )
        }
    }

    private fun buildComponents(
        packageName: String,
        activities: Array<android.content.pm.ActivityInfo>,
        services: Array<android.content.pm.ServiceInfo>,
        receivers: Array<android.content.pm.ActivityInfo>,
        providers: Array<android.content.pm.ProviderInfo>
    ): List<AppComponent> {
        val monitoredActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_PACKAGE_ADDED,
            "android.net.conn.CONNECTIVITY_CHANGE",
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
        return activities.map {
            AppComponent(it.name, ComponentType.ACTIVITY, it.exported, it.enabled)
        } + services.map {
            AppComponent(it.name, ComponentType.SERVICE, it.exported, it.enabled)
        } + receivers.map { receiver ->
            val actions = monitoredActions.filter { action ->
                pm.queryBroadcastReceivers(Intent(action).setPackage(packageName), PackageManager.MATCH_ALL)
                    .any { it.activityInfo.name == receiver.name }
            }
            AppComponent(receiver.name, ComponentType.RECEIVER, receiver.exported, receiver.enabled, actions)
        } + providers.map {
            AppComponent(it.name, ComponentType.PROVIDER, it.exported, it.isEnabled)
        }
    }

    fun setComponentEnabled(packageName: String, componentName: String, enabled: Boolean) {
        pm.setComponentEnabledSetting(
            ComponentName(packageName, componentName),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
