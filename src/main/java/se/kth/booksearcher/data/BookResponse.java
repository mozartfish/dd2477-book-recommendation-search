package se.kth.booksearcher.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// more stuff will be here later
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookResponse(
    @NotNull String title,
    @NotNull String author,
    float rating,
    @NotNull String imageUrl,
    @NotNull String description,
    List<String> genres,
    int pages,
    List<Integer> reviews_stars,
    List<String> reviews_text) {}
