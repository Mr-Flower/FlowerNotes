package com.flowernotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowernotes.data.EventoData
import com.flowernotes.ui.confirm.ConfirmScreen
import com.flowernotes.ui.home.HomeScreen
import com.flowernotes.ui.info.InfoScreen
import com.flowernotes.ui.list.ListScreen
import com.flowernotes.ui.manual.ManualScreen
import com.flowernotes.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val MANUAL = "manual"
    const val LIST = "list"
    const val SETTINGS = "settings"
    const val INFO = "info"
    const val CONFIRM = "confirm/{eventoJson}"

    fun confirm(evento: EventoData) = "confirm/${evento.toNavArg()}"
}

@Composable
fun FlowerNotesApp(startListenTrigger: Int = 0) {
    val navController = rememberNavController()

    // Il Quick Tile porta sempre alla Home, dove parte l'ascolto
    LaunchedEffect(startListenTrigger) {
        if (startListenTrigger > 0) {
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onEventExtracted = { evento -> navController.navigate(Routes.confirm(evento)) },
                onOpenManual = { navController.navigate(Routes.MANUAL) },
                onOpenList = { navController.navigate(Routes.LIST) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                startListenTrigger = startListenTrigger,
            )
        }
        composable(Routes.MANUAL) {
            ManualScreen(
                onEventExtracted = { evento ->
                    navController.navigate(Routes.confirm(evento)) {
                        // Il flusso manuale si chiude quando si arriva alla conferma
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.CONFIRM,
            arguments = listOf(navArgument("eventoJson") { type = NavType.StringType }),
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("eventoJson") ?: "{}"
            ConfirmScreen(
                initialEvento = EventoData.fromJson(json),
                onSaved = {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.HOME)
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Routes.LIST) {
            ListScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenInfo = { navController.navigate(Routes.INFO) },
            )
        }
        composable(Routes.INFO) {
            InfoScreen(onBack = { navController.popBackStack() })
        }
    }
}
