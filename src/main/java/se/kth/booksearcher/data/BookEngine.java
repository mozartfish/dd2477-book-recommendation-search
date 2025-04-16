package se.kth.booksearcher.data;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookEngine implements SearchEngine{
    String serverUrl = "http://localhost:9200"; //todo: why does it need ot be http and not https?
    String apiKey = "eVppQ1BwWUJ2dW1jSVBleHZidGc6UUs1dzZQOFF0emZuWFNsQV93ZnR1UQ==";
    ElasticsearchClient esClient;
    List<BookResponse> cachedReadBooks = new ArrayList<>();

    public BookEngine() {
        esClient = ElasticsearchClient.of(b -> b
                .host(serverUrl)
                .apiKey(apiKey)
        );
    }

    @Override
    public void setProfile(UserProfile userProfile) {
        List<String> ids = userProfile.readBooks().stream().toList();
        var idsQuery = IdsQuery.of(idq -> idq.values(ids));
        try {
            SearchResponse<BookResponse> searchResult = esClient.search(s -> s
                    .index(("books"))
                    .query(q -> q.ids(idsQuery)), BookResponse.class);
            cachedReadBooks = new ArrayList<>();
            for (Hit<BookResponse> hit : searchResult.hits().hits()) {
                cachedReadBooks.add(hit.source());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public List<Book> search(String query) {
        List<Book> result = new ArrayList<>();
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
            for (Hit<BookResponse> hit : searchResult.hits().hits()) {
                assert hit.source() != null;
                result.add(new Book(hit.source(), hit.id()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public void addReadBook(String id) {
        try {
            SearchResponse<BookResponse> searchResult = esClient.search(s -> s
                    .index(("books"))
                    .size(1)
                    .query(q -> q.ids(IdsQuery.of(iq -> iq.values(id)))), BookResponse.class);
            List<Hit<BookResponse>> hits = searchResult.hits().hits();
            if (hits.size() != 1) {
                throw new Exception("Could not find book by id");
            }
            cachedReadBooks.add(hits.getFirst().source());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
