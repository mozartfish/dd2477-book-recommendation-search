package se.kth.booksearcher.ui

data class SearchUiState(
    val isProgressing: Boolean = false,
    val books: List<Book> = emptyList(),
)
