package se.kth.booksearcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import se.kth.booksearcher.ui.App
import se.kth.booksearcher.ui.ProfileManager

var userProfile by mutableStateOf(loadProfile("Default"))
val searchEngine = FakeSearchEngine

fun main() = application {
    var switchingProfile by remember { mutableStateOf(false) }

    val windowState = rememberWindowState(size = DpSize(800.dp, 600.dp),)
    Window(
        onCloseRequest = ::exitApplication,
        title = "BookSearcher",
        state = windowState,
    ) {

        MenuBar {
            Menu("Profile") {
                Item("Current: ${userProfile.username}", onClick = {})
                Item("Switch profile", onClick = { switchingProfile = true })
            }
        }
        App(windowState)
    }

    if (switchingProfile) {
        Window(
            onCloseRequest = { switchingProfile = false },
            title = "Switch Profile",
            state = rememberWindowState(size = DpSize(400.dp, 600.dp)),
        ) {
            ProfileManager {
                switchingProfile = false
            }
        }
    }
}
