package com.example.pushup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pushup.ui.screens.AdminSeedScreen
import com.example.pushup.ui.screens.CreateExerciseScreen
import com.example.pushup.ui.screens.CreateWorkoutAIScreen
import com.example.pushup.ui.screens.CreateWorkoutChoiceScreen
import com.example.pushup.ui.screens.CreateWorkoutManualScreen
import com.example.pushup.ui.screens.WorkoutDetailScreen
import com.example.pushup.viewmodels.AuthState
import com.example.pushup.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    
    when (authState) {
        is AuthState.Loading -> {
            // Show loading screen
            Surface(
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Loading...")
            }
        }
        is AuthState.Unauthenticated -> {
            AuthNavigation(authViewModel)
        }
        is AuthState.Authenticated -> {
            MiniApp(authViewModel)
        }
    }
}

@Composable
fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {
                    // Will automatically navigate to main app when auth state changes
                }
            )
        }
        composable("register") {
            RegistrationScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    // Will automatically navigate to main app when auth state changes
                }
            )
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
fun MiniApp(authViewModel: AuthViewModel = viewModel()) {
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
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.DarkGray,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            when (currentRoute) {
                TopDest.Workouts.route -> {
                    FloatingActionButton(
                        onClick = { navController.navigate("createWorkoutChoice") }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Workout")
                    }
                }
                TopDest.Exercises.route -> {
                    FloatingActionButton(
                        onClick = { navController.navigate("createExercise") }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Exercise")
                    }
                }
            }
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
            composable(TopDest.Home.route) { HomeScreen(vm, navController) }
            composable(TopDest.Workouts.route) { WorkoutScreen(navController) }
            composable(TopDest.Exercises.route) { ExerciseScreen() }
            composable(TopDest.Statistic.route) { StatisticScreen(vm) }
            composable("adminSeed") { AdminSeedScreen() }
            composable("createWorkoutChoice") { CreateWorkoutChoiceScreen(navController) }
            composable("createWorkoutManual") { CreateWorkoutManualScreen(navController) }
            composable("createWorkoutAI") { CreateWorkoutAIScreen(navController) }
            composable("createExercise") { CreateExerciseScreen(navController) }
            composable(
                route = "workoutDetail/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                WorkoutDetailScreen(workoutId = workoutId, navController = navController)
            }
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