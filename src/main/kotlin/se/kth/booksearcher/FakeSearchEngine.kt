package se.kth.booksearcher

import se.kth.booksearcher.data.Book
import se.kth.booksearcher.data.SearchEngine
import se.kth.booksearcher.data.UserProfile

object FakeSearchEngine : SearchEngine {
    override fun setProfile(userProfile: UserProfile) {
    }

    override fun search(query: String): List<Book> {
        Thread.sleep(500)
        return if (query.isBlank()) {
            emptyList()
        } else {
            listOf(
                Book(
                    "The Anubis Gates",
                    "Tim Powers",
                    3.9f,
                    "Brendan Doyle, a specialist in the work of the early-nineteenth century poet William Ashbless, reluctantly accepts an invitation from a millionaire to act as a guide to time-travelling tourists. But while attending a lecture given by Samuel Taylor Coleridge in 1810, he becomes marooned in Regency London, where dark and dangerous forces know about the gates in time.",
                    "https://images-na.ssl-images-amazon.com/images/S/compressed.photo.goodreads.com/books/1344409006i/142296.jpg",
                    "1"
                ),
                Book("Book 2", "", 5f, "", "", "2"),
                Book("Book 3", "", 5f, "", "", "3"),
                Book("Book 4", "", 5f, "", "", "4"),
                Book("Book 5", "", 5f, "", "", "5"),
                Book("Book 6", "", 5f, "", "", "6"),
                Book("Book 7", "", 5f, "", "", "7"),
            )
        }
    }

    override fun addReadBook(id: String) {
    }

    override fun removeReadBook(id: String) {
    }
}
