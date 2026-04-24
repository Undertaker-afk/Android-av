package com.youravapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youravapp.domain.model.InstalledApp
import com.youravapp.domain.model.SandboxAnalysis
import com.youravapp.domain.repository.IAppRepository
import com.youravapp.domain.usecase.GetInstalledAppsUseCase
import com.youravapp.domain.usecase.RunSandboxForNewAppUseCase
import com.youravapp.domain.usecase.ToggleComponentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val toggleComponentUseCase: ToggleComponentUseCase,
    private val sandboxUseCase: RunSandboxForNewAppUseCase,
    private val appRepository: IAppRepository
) : ViewModel() {
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _whitelist = MutableStateFlow<Set<String>>(emptySet())
    val whitelist = _whitelist.asStateFlow()

    private val _sandboxResults = MutableStateFlow<Map<String, SandboxAnalysis>>(emptyMap())
    val sandboxResults = _sandboxResults.asStateFlow()

    private val _sandboxing = MutableStateFlow<Set<String>>(emptySet())
    val sandboxing = _sandboxing.asStateFlow()

    init {
        viewModelScope.launch {
            appRepository.whitelistPackages().collect { _whitelist.value = it }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _apps.value = getInstalledAppsUseCase()
        }
    }

    fun toggle(packageName: String, component: String, enabled: Boolean) {
        viewModelScope.launch { toggleComponentUseCase(packageName, component, enabled) }
    }

    fun toggleWhitelist(packageName: String) {
        viewModelScope.launch {
            if (_whitelist.value.contains(packageName)) appRepository.removeFromWhitelist(packageName)
            else appRepository.addToWhitelist(packageName)
        }
    }

    fun runSandbox(packageName: String) {
        viewModelScope.launch {
            _sandboxing.value = _sandboxing.value + packageName
            sandboxUseCase(packageName).onSuccess { analysis ->
                _sandboxResults.value = _sandboxResults.value + (packageName to analysis)
            }
            _sandboxing.value = _sandboxing.value - packageName
        }
    }
}
