package com.youravapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youravapp.domain.model.ThreatAction
import com.youravapp.viewmodel.AppListViewModel
import com.youravapp.viewmodel.ScanViewModel

@Composable
fun DashboardScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Zero-Trust Trinity-AV")
        Text("Ransomware + RAT heuristics, archive cracking, controlled folder policy, and app whitelist controls")
    }
}

@Composable
fun ScanScreen(viewModel: ScanViewModel = hiltViewModel()) {
    val detection by viewModel.pendingDetection.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = viewModel::startScan) { Text("Start full scan") }
        detection?.let {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Suspicious item detected")
                    Text(it.filePath)
                    Text("Threat: ${it.threatName}")
                    Text("Category: ${it.category} | Confidence: ${it.confidence}%")
                    Button(onClick = { viewModel.onDetectionDecision(ThreatAction.QUARANTINE) }) { Text("Quarantine") }
                    Button(onClick = { viewModel.onDetectionDecision(ThreatAction.IGNORE) }) { Text("Ignore") }
                    Button(onClick = { viewModel.onDetectionDecision(ThreatAction.DELETE) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun AppsScreen(viewModel: AppListViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsState()
    val whitelist by viewModel.whitelist.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadApps() }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(apps) { app ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(app.appName)
                    Text(app.packageName)
                    Text("Components: ${app.components.size}")
                    Text(if (whitelist.contains(app.packageName)) "Trusted by whitelist" else "Untrusted")
                    Button(onClick = { viewModel.toggleWhitelist(app.packageName) }) {
                        Text(if (whitelist.contains(app.packageName)) "Remove from whitelist" else "Add to whitelist")
                    }
                }
            }
        }
    }
}

@Composable fun QuarantineScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Quarantine list (encrypted entries)") } }
@Composable fun SettingsScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Settings: exclusions, DB update, controlled folder access") } }
