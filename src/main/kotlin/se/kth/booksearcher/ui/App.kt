package se.kth.booksearcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = remember { SearchViewModel() }
        val uiState by viewModel.uiState.collectAsState()
        val scope = rememberCoroutineScope()
        var searchText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "There should be a logo",
                modifier = Modifier.padding(16.dp),
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .onKeyEvent {
                    if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                        scope.launch { viewModel.launchSearch(searchText) }
                        true
                    } else {
                        false
                    }
                },
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
            )
            AnimatedVisibility(uiState.isProgressing) {
                LinearProgressIndicator(Modifier.fillMaxWidth(0.7f).padding(top = 16.dp))
            }
            AnimatedVisibility(uiState.books.isNotEmpty()) {
                LazyColumn(modifier = Modifier.padding(top = 24.dp)) {
                    items(uiState.books) { book ->
                        ListItem(
                            headlineContent = { Text(book.name) },
                            supportingContent = { Text("Placeholder") }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
