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
            Book("The Anubis Gates", "Tim Powers", 3.9f, "Brendan Doyle, a specialist in the work of the early-nineteenth century poet William Ashbless, reluctantly accepts an invitation from a millionaire to act as a guide to time-travelling tourists. But while attending a lecture given by Samuel Taylor Coleridge in 1810, he becomes marooned in Regency London, where dark and dangerous forces know about the gates in time.", "https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1344409006i/142296.jpg"),
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
