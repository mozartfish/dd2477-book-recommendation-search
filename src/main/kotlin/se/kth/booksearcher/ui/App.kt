package se.kth.booksearcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import kotlinx.coroutines.launch
import se.kth.booksearcher.data.Book

@Composable
@Preview
fun App(windowState: WindowState) {
    var book by remember { mutableStateOf<Book?>(null) }
    val targetWidth by animateDpAsState(
        targetValue = if (book != null) 1200.dp else 800.dp,
        animationSpec = tween(500),
        label = "WindowWidth",
    )

    LaunchedEffect(targetWidth) {
        windowState.size = DpSize(targetWidth, windowState.size.height)
    }

    MaterialTheme {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxHeight().width(800.dp)) {
                SearchPage {
                    book = it
                }
            }

            AnimatedVisibility(
                visible = book != null,
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            ) {
                BookDetailPage(book!!)
            }
        }
    }
}

@Composable
@Preview
fun SearchPage(onBookClick: (Book) -> Unit) {
    val viewModel = remember { SearchViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
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
                        modifier = Modifier.clickable { onBookClick(book) },
                        headlineContent = { Text(book.name) },
                        supportingContent = { Text("Placeholder") }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun BookDetailPage(
    book: Book,
    onClose: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        if (book.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(book.imageUrl)
                    .crossfade(true)
                    .size(Size.ORIGINAL)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp),
            )
        }

        Text(
            book.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            "Author: ${book.author}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Rating: ${book.rating}",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Introduction",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            book.introduction,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
