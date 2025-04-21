package se.kth.booksearcher.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;



public interface SearchEngine {
    void setProfile(@NotNull UserProfile userProfile);
    List<Book> search(@NotNull String query);
    void addReadBook(@NotNull String id);
}
