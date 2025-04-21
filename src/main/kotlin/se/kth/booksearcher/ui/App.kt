package se.kth.booksearcher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.constraintlayout.compose.ConstraintLayout
import booksearcher.generated.resources.Res
import booksearcher.generated.resources.logo
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import se.kth.booksearcher.data.Book
import se.kth.booksearcher.data.UserProfile
import se.kth.booksearcher.saveProfile
import se.kth.booksearcher.userProfile

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
                book?.let {
                    BookDetailPage(
                        book = it,
                        onClose = { book = null },
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun SearchPage(onBookClick: (Book) -> Unit) {
    val viewModel = remember { SearchViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val hasSearchResults = uiState.books.isNotEmpty()
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize().padding(48.dp),
    ) {
        val (logo, searchField, progressBar, books) = createRefs()
        AnimatedVisibility(
            visible = !hasSearchResults,
            enter = slideInVertically(tween(500)),
            exit = slideOutVertically(tween(500)),
            modifier = Modifier
                .height(160.dp)
                .constrainAs(logo) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(searchField.top)
                }
        ) {
            Image(
                painterResource(Res.drawable.logo),
                contentDescription = "Logo",
            )
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .constrainAs(searchField) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    if (!hasSearchResults) {
                        bottom.linkTo(parent.bottom)
                    } else {
                        bottom.linkTo(books.top)
                    }
                }
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
        AnimatedVisibility(
            visible = uiState.isProgressing,
            modifier = Modifier.constrainAs(progressBar) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(searchField.bottom)
            }
        ) {
            LinearProgressIndicator(Modifier.fillMaxWidth(0.7f).padding(top = 16.dp))
        }
        AnimatedVisibility(
            visible = hasSearchResults,
            modifier = Modifier.constrainAs(books) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(searchField.bottom)
                bottom.linkTo(parent.bottom)
            }
        ) {
            LazyColumn(modifier = Modifier.padding(top = 24.dp)) {
                items(uiState.books) { book ->
                    val isRead = remember(book, userProfile) { book.id in userProfile.readBooks }
                    ListItem(
                        modifier = Modifier.clickable { onBookClick(book) },
                        headlineContent = { Text(book.name) },
                        supportingContent = { Text(book.author) },
                        trailingContent = {
                            Button(
                                onClick = {
                                    userProfile = UserProfile(
                                        userProfile.username,
                                        when (isRead) {
                                            true -> userProfile.readBooks - book.id
                                            false -> userProfile.readBooks + book.id
                                        }
                                    )
                                    saveProfile(userProfile)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRead)
                                        MaterialTheme.colorScheme.secondary
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Mark as read")
                            }
                        }
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
        Box(Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }

            if (book.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(book.imageUrl)
                        .crossfade(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = "Book cover",
                    modifier = Modifier
                        .height(220.dp)
                        .padding(vertical = 16.dp)
                        .align(Alignment.Center),
                )
            }
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
