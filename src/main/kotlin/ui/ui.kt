package org.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.viewmodel.compose.viewModel

fun main() = application {
	Window(
		onCloseRequest = ::exitApplication,
		title = "MATRX GIPF"
	) { App(window = this.window) }
}

@Composable
fun App(window: ComposeWindow) {



	val coroutineScope = rememberCoroutineScope()

	MaterialTheme {
		MainApp(window = window)
	}
}

@Composable
fun MainApp(
	window: ComposeWindow,
	viewModel: MainViewModel = viewModel { MainViewModel() },
//	navController: NavHostController = rememberNavController()
) {

	val uiState by viewModel.uiState.collectAsState()

	MainScreen(
		window = window,
		uiState = uiState,
		onEvent = { event -> viewModel.onEvent(event) },
	)
}


val Ultramarine40 = Color(0xFF648FFF) // #648fff
val Indigo50 = Color(0xFF785EF0) // #785ef0
val Magenta50 = Color(0xFFDC267F) // #dc267f
val Orange40 = Color(0xFFFE6100) // #fe6100
val Gold20 = Color(0xFFFFB000) // #ffb000

object IBMColorBlindPalette {
	val colors = listOf(
		Ultramarine40,
		Indigo50,
		Magenta50,
		Orange40,
		Gold20
	)
	private var currentIndex = 0
	fun next(): Color { // Simple sequential assignment for stability
		val color = colors[currentIndex % colors.size]
		currentIndex++
		return color
	}

	// Or, implement a stable random based on an ID/hash if preferred
	fun stableColorFor(id: Any): Color {
		val hashCode = id.hashCode()
		return colors[kotlin.math.abs(hashCode) % colors.size]
	}
}



