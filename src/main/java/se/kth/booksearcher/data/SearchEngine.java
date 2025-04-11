package se.kth.booksearcher.data;

import java.util.List;

public interface SearchEngine {
    List<Book> search(String query);
}
