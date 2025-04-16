package se.kth.booksearcher.data;

import java.util.List;



public interface SearchEngine {
    void setProfile(UserProfile userProfile);
    List<Book> search(String query);
    void addReadBook(String id);
}
