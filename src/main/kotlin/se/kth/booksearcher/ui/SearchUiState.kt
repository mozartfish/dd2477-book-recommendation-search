package se.kth.booksearcher.ui

import se.kth.booksearcher.data.Book

data class SearchUiState(
    val isProgressing: Boolean = false,
    val books: List<Book> = emptyList(),
)
