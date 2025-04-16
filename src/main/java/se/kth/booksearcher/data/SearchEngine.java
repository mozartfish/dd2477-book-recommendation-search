package se.kth.booksearcher.data;

import org.elasticsearch.client.RestHighLevelClient;

import java.util.List;

public interface SearchEngine {
    List<Book> search(RestHighLevelClient client, String query);
}
