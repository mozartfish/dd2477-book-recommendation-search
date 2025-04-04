package se.kth.booksearcher.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    suspend fun launchSearch(searchText: String) {
        _uiState.value = _uiState.value.copy(isProgressing = true)
        // TODO: Retrieve books from the search engine
        delay(1000)
        val books = listOf(
            Book("Book 1"),
            Book("Book 2"),
            Book("Book 3"),
            Book("Book 4"),
            Book("Book 5"),
            Book("Book 6"),
            Book("Book 7"),
        )
        _uiState.value = SearchUiState(
            isProgressing = false,
            books = books,
        )
    }
}
