package com.flowernotes.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    const val MANUAL = "manual?text={text}"
    const val LIST = "list"
    const val SETTINGS = "settings"
    const val INFO = "info"
    const val CONFIRM = "confirm/{eventiJson}"
    const val EDIT = "edit/{eventId}"

    fun manual(text: String = "") = "manual?text=${Uri.encode(text)}"
    fun confirm(eventi: List<EventoData>) = "confirm/${EventoData.listToNavArg(eventi)}"
    fun edit(eventId: Long) = "edit/$eventId"
}

@Composable
fun FlowerNotesApp(
    startListenTrigger: Int = 0,
    sharedTextTrigger: Int = 0,
    sharedText: String = "",
) {
    val navController = rememberNavController()

    // Il Quick Tile porta sempre alla Home, dove parte l'ascolto
    LaunchedEffect(startListenTrigger) {
        if (startListenTrigger > 0) {
            navController.popBackStack(Routes.HOME, inclusive = false)
        }
    }

    // Testo condiviso da un'altra app: apre l'inserimento manuale precompilato
    LaunchedEffect(sharedTextTrigger) {
        if (sharedTextTrigger > 0 && sharedText.isNotBlank()) {
            navController.popBackStack(Routes.HOME, inclusive = false)
            navController.navigate(Routes.manual(sharedText))
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onEventExtracted = { eventi -> navController.navigate(Routes.confirm(eventi)) },
                onOpenManual = { navController.navigate(Routes.manual()) },
                onOpenList = { navController.navigate(Routes.LIST) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                startListenTrigger = startListenTrigger,
            )
        }
        composable(
            route = Routes.MANUAL,
            arguments = listOf(navArgument("text") {
                type = NavType.StringType
                defaultValue = ""
            }),
        ) { backStackEntry ->
            ManualScreen(
                onEventExtracted = { eventi ->
                    navController.navigate(Routes.confirm(eventi)) {
                        // Il flusso manuale si chiude quando si arriva alla conferma
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
                initialText = backStackEntry.arguments?.getString("text") ?: "",
            )
        }
        composable(
            route = Routes.CONFIRM,
            arguments = listOf(navArgument("eventiJson") { type = NavType.StringType }),
        ) { backStackEntry ->
            val json = backStackEntry.arguments?.getString("eventiJson") ?: "[]"
            // remember: niente ri-parsing del JSON a ogni ricomposizione
            val eventi = remember(json) { EventoData.listFromJson(json) }
            ConfirmScreen(
                initialEventi = eventi,
                editEventId = null,
                onSaved = {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.HOME)
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
        ) { backStackEntry ->
            ConfirmScreen(
                initialEventi = emptyList(),
                editEventId = backStackEntry.arguments?.getLong("eventId"),
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Routes.LIST) {
            ListScreen(
                onBack = { navController.popBackStack() },
                onEdit = { event -> navController.navigate(Routes.edit(event.id)) },
            )
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
