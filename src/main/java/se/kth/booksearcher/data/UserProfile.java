package se.kth.booksearcher.data;

import java.util.Set;

public record UserProfile(
        String username,
        Set<String> readBooks
) {
}
