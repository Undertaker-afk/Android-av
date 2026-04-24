package com.youravapp.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val skipLargeFiles = booleanPreferencesKey("skip_large_files")
    private val whitelistPackages = stringSetPreferencesKey("whitelist_packages")
    private val excludedPaths = stringSetPreferencesKey("excluded_paths")

    val skipLargeFilesFlow: Flow<Boolean> = context.dataStore.data.map { it[skipLargeFiles] ?: true }
    val whitelistPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { it[whitelistPackages] ?: emptySet() }
    val excludedPathsFlow: Flow<Set<String>> = context.dataStore.data.map { it[excludedPaths] ?: emptySet() }

    suspend fun setSkipLargeFiles(value: Boolean) {
        context.dataStore.edit { it[skipLargeFiles] = value }
    }

    suspend fun addWhitelistPackage(packageName: String) = editSet(whitelistPackages) { it + packageName }
    suspend fun removeWhitelistPackage(packageName: String) = editSet(whitelistPackages) { it - packageName }

    suspend fun addExcludedPath(path: String) = editSet(excludedPaths) { it + path }
    suspend fun removeExcludedPath(path: String) = editSet(excludedPaths) { it - path }

    private suspend fun editSet(
        key: Preferences.Key<Set<String>>,
        transform: (Set<String>) -> Set<String>
    ) {
        context.dataStore.edit { pref ->
            pref[key] = transform(pref[key] ?: emptySet())
        }
    }
}
