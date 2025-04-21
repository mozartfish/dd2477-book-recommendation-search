package se.kth.booksearcher.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import se.kth.booksearcher.searchEngine

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    suspend fun launchSearch(searchText: String) {
        _uiState.value = _uiState.value.copy(isProgressing = true)
        val books = withContext(Dispatchers.IO) {
            searchEngine.search(searchText)
        }
        _uiState.value = SearchUiState(
            isProgressing = false,
            books = books,
        )
    }
}
