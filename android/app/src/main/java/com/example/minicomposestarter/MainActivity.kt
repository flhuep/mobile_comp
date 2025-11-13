package com.example.minicomposestarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MiniApp()
                }
            }
        }
    }
}

private enum class TopDest(val route: String, val label: String) {
    Home("home", "Home"),
    Workouts("workouts", "Workouts"),
    Exercises("exercises", "Exercises"),
    Statistic("statistic", "Statistic")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniApp() {
    val navController = rememberNavController()

    // Shared ViewModel instance for all destinations:
    val vm: CounterViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TopDest.Home.route
    val tabs = listOf(TopDest.Home, TopDest.Workouts, TopDest.Exercises, TopDest.Statistic)
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PushUp") },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.pushup_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(50.dp)
                            .padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.DarkGray,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.DarkGray,
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        text = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDest.Home.route) { HomeScreen(vm) }
            composable(TopDest.Workouts.route) { WorkoutScreen(vm) }
            composable(TopDest.Exercises.route) { ExerciseScreen(vm) }
            composable(TopDest.Statistic.route) { StatisticScreen(vm) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMiniApp() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MiniApp() // preview-only instance
        }
    }
}