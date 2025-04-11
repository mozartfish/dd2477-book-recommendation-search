package se.kth.booksearcher.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import se.kth.booksearcher.data.Book

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    suspend fun launchSearch(searchText: String) {
        _uiState.value = _uiState.value.copy(isProgressing = true)
        // TODO: Retrieve books from the search engine
        delay(1000)
        val books = listOf(
            Book("Book 1", "", 5f, "", ""),
            Book("Book 2", "", 5f, "", ""),
            Book("Book 3", "", 5f, "", ""),
            Book("Book 4", "", 5f, "", ""),
            Book("Book 5", "", 5f, "", ""),
            Book("Book 6", "", 5f, "", ""),
            Book("Book 7", "", 5f, "", ""),
        )
        _uiState.value = SearchUiState(
            isProgressing = false,
            books = books,
        )
    }
}
