package se.kth.booksearcher.data;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookEngine implements SearchEngine {
    String serverUrl = "http://localhost:9200"; //todo: why does it need ot be http and not https?
    //    String apiKey = "";
    ElasticsearchClient esClient;

    // List of Pairs, first component being the ID and second component being the book information
    List<Pair<String, BookResponse>> cachedReadBooks = new ArrayList<>();

    public BookEngine() {
        esClient = ElasticsearchClient.of(b -> b
                        .host(serverUrl)
//                        .usernameAndPassword("elastic", "password")
//                .apiKey() alternative
        );
    }

    @Override
    public void setProfile(@NotNull UserProfile userProfile) {
        List<String> ids = userProfile.readBooks().stream().toList();
        var idsQuery = IdsQuery.of(idq -> idq.values(ids));
        try {
            SearchResponse<BookResponse> searchResult = esClient.search(s -> s
                    .index(("books"))
                    .query(q -> q.ids(idsQuery)), BookResponse.class);
            cachedReadBooks = new ArrayList<>();
            for (Hit<BookResponse> hit : searchResult.hits().hits()) {
                cachedReadBooks.add(new Pair<>(hit.id(), hit.source()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public @NotNull List<Book> search(@NotNull String query) {
        try {
            SearchResponse<BookResponse> searchResult = esClient.search(s -> s
                            .index("books")
                            .query(q -> q
                                    .simpleQueryString(sqs -> sqs
                                            .query(query)
                                    )
                            ),
                    BookResponse.class
            );
            return relevanceFeedback(searchResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Relevance feedback based on results that elasticsearch provided from the querystring
    // The order of the returned list is the order that will show up in the UI
    // If you want to do relevance feedback directly in the query (like assignment 3) you will have to modify the search function instaed of this one
    private @NotNull List<Book> relevanceFeedback(SearchResponse<BookResponse> searchResult) {
        //temporary, keeps same order as elasticsearch provided
        return searchResult.hits().hits().stream().map(b -> {
            assert b.source() != null; // keep typechecker happy
            return new Book(b.source(), b.id());
        }).toList();
    }

    @Override
    public void addReadBook(@NotNull String id) {
        try {
            GetResponse<BookResponse> response = esClient.get(g -> g
                    .index("books")
                    .id(id), BookResponse.class);
            if (response.found()) {
                cachedReadBooks.add(new Pair<>(response.id(), response.source()));
            } else {
                throw new Exception("Could not find book by id");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeReadBook(@NotNull String id) {
        cachedReadBooks.removeIf(bookIdPair -> bookIdPair.component1().equals(id));
    }
}
