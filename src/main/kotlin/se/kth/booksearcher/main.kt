package se.kth.booksearcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import se.kth.booksearcher.data.BookEngine
import se.kth.booksearcher.ui.App

var userProfile by mutableStateOf(loadProfile("Default"))
val searchEngine = BookEngine()
//val searchEngine = FakeSearchEngine

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(800.dp, 600.dp),)
    Window(
        onCloseRequest = ::exitApplication,
        title = "BookSearcher",
        state = windowState,
    ) {
        MaterialTheme {
            App(windowState)
        }
    }
}
