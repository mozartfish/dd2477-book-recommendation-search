package se.kth.booksearcher.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookResponse(
        @NotNull String title,
        @NotNull String author,
        float rating,
        @NotNull String imageUrl,
        @NotNull String description
) {
}
