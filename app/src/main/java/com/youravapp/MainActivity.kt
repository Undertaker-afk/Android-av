package com.youravapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.youravapp.ui.screens.AppsScreen
import com.youravapp.ui.screens.DashboardScreen
import com.youravapp.ui.screens.QuarantineScreen
import com.youravapp.ui.screens.ScanScreen
import com.youravapp.ui.screens.SettingsScreen
import com.youravapp.ui.theme.AvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AvTheme { MainNavigation() } }
    }
}

private data class NavItem(val route: String, val label: String)

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        NavItem("dashboard", "Dashboard"),
        NavItem("scan", "Scan"),
        NavItem("apps", "Apps"),
        NavItem("quarantine", "Quarantine"),
        NavItem("settings", "Settings")
    )

    Scaffold(bottomBar = {
        val current by navController.currentBackStackEntryAsState()
        NavigationBar {
            items.forEach {
                NavigationBarItem(
                    selected = current?.destination?.route == it.route,
                    onClick = { navController.navigate(it.route) },
                    icon = { Text(it.label.take(1)) },
                    label = { Text(it.label) }
                )
            }
        }
    }) { padding ->
        NavHost(navController = navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen() }
            composable("scan") { ScanScreen() }
            composable("apps") { AppsScreen() }
            composable("quarantine") { QuarantineScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
