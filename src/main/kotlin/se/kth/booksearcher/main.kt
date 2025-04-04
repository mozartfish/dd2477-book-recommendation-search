package se.kth.booksearcher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import se.kth.booksearcher.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BookSearcher",
    ) {
        App()
    }
}
