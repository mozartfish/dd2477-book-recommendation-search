package se.kth.booksearcher.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// more stuff will be here later
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookResponse(
    @NotNull String title,
    @NotNull String author,
    float rating,
    @NotNull String imageUrl,
    @NotNull String description,
    @Nullable List<String> genres,
    @Nullable Long firstPublished,
    @Nullable Integer ratingsCount) {}
