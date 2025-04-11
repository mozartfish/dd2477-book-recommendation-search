package se.kth.booksearcher.data;

import org.jetbrains.annotations.NotNull;

public record Book(
        @NotNull String name,
        @NotNull String author,
        float rating,
        @NotNull String introduction,
        @NotNull String imageUrl
) {
}
