package com.example.prepyy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MyAppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.observeAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus()
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onNavigateToSignIn = { navController.navigate("signin") },
                onNavigateToMain = { navController.navigate("main") }
            )
        }
        composable("signin") {
            SignInScreen(
                onSignIn = { email, password ->
                    authViewModel.login(email, password)
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignUp = { email, password ->
                    authViewModel.signup(email, password)
                },
                onNavigateToSignIn = { navController.navigate("signin") }
            )
        }
        composable("main") {
            MainScreen(
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("signin")
                }
            )
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> navController.navigate("main")
            is AuthState.Unauthenticated -> navController.navigate("signin")
            is AuthState.Error -> {
                // Handle error, maybe show a snackbar or dialog
            }
            else -> {} // Do nothing for Loading state
        }
    }
}

@Composable
fun SplashScreen(onNavigateToSignIn: () -> Unit, onNavigateToMain: () -> Unit) {
    // Implement your splash screen UI
}

@Composable
fun SignInScreen(onSignIn: (String, String) -> Unit, onNavigateToSignUp: () -> Unit) {
    // Implement your sign in screen UI using the layout from paste-2.txt
}

@Composable
fun SignUpScreen(onSignUp: (String, String) -> Unit, onNavigateToSignIn: () -> Unit) {
    // Implement your sign up screen UI using the layout from paste.txt
}

@Composable
fun MainScreen(onSignOut: () -> Unit) {
    // Implement your main screen UI
}