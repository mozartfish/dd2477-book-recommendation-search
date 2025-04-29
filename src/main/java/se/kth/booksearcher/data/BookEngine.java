package se.kth.booksearcher.data;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookEngine implements SearchEngine {
  String serverUrl = "http://localhost:9200"; // todo: why does it need ot be http and not https?
  //    String apiKey = "";
  ElasticsearchClient esClient;

  // List of Pairs, first component being the ID and second component being the book information
  List<Pair<String, BookResponse>> cachedReadBooks = new ArrayList<>();

  public BookEngine() {
    esClient =
        ElasticsearchClient.of(
            b -> b.host(serverUrl)
            //                        .usernameAndPassword("elastic", "password")
            //                .apiKey() alternative
            );
  }

  @Override
  public void setProfile(@NotNull UserProfile userProfile) {
    List<String> ids = userProfile.readBooks().stream().toList();
    var idsQuery = IdsQuery.of(idq -> idq.values(ids));
    try {
      SearchResponse<BookResponse> searchResult =
          esClient.search(s -> s.index(("books")).query(q -> q.ids(idsQuery)), BookResponse.class);
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
      for (Pair<String, BookResponse> cachedReadBook : cachedReadBooks) {
        System.out.println(cachedReadBook.component1());
      }

      // boolean query builder
      BoolQuery.Builder booleanQueryBuilder = new BoolQuery.Builder();

      // simple query
      SimpleQueryStringQuery simpleQuery = buildSimpleStringQuery(query);
      booleanQueryBuilder.should(simpleQuery._toQuery());

      // if user has read previous books
      if (!cachedReadBooks.isEmpty()) {
        List<String> readBookIDs = cachedReadBooks.stream().map(pair -> pair.component1()).toList();
        MoreLikeThisQuery enhancedQuery = buildMoreLikeThisQuery(readBookIDs);
        booleanQueryBuilder.should(enhancedQuery._toQuery());
      }

      // build query
      Query searchQuery = new Query.Builder().bool(booleanQueryBuilder.build()).build();

      // search request
      SearchResponse<BookResponse> searchRequest =
          esClient.search(builder -> builder.index("books").query(searchQuery), BookResponse.class);

      // process query and return result
      return queryResult(searchRequest);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Process the elastic search query to return a list of books
   *
   * @param searchRequest information requested by user (book response format)
   * @return convert the information retrieved from elastic search to a list
   */
  private @NotNull List<Book> queryResult(SearchResponse<BookResponse> searchRequest) {
    return searchRequest.hits().hits().stream()
        .map(
            hit -> {
              BookResponse source = hit.source();
              assert source != null;
              return new Book(source, hit.id());
            })
        .toList();
  }

  /**
   * Construct a more like this query to refine the results based on the previous books the user has
   * read
   *
   * @param readBookIDs ids of the books the user has read
   * @return a more like this query
   */
  private MoreLikeThisQuery buildMoreLikeThisQuery(List<String> readBookIDs) {
    List<Like> likeDocuments =
        readBookIDs.stream()
            .map(
                id -> {
                  LikeDocument document = new LikeDocument.Builder().index("books").id(id).build();
                  return Like.of(l -> l.document(document));
                })
            .toList();

    return new MoreLikeThisQuery.Builder()
        .fields(List.of("author", "genres", "description", "rating", "reviews.txt"))
        .like(likeDocuments)
        .minTermFreq(1) // include terms that appear at least once
        .maxQueryTerms(25) // get the top 25 most relevant terms to influence the search
        .minimumShouldMatch("30%") // results contain at least 30% of the query terms
        .boost(0.5F)
        .build();
  }

  /**
   * builds a general boolean text query for a broad text search
   *
   * @param query - the request by the user
   * @return elastic search simple query
   */
  private SimpleQueryStringQuery buildSimpleStringQuery(@NotNull String query) {
    return new SimpleQueryStringQuery.Builder()
        .fields(List.of("author", "description", "genres", "title", "rating", "reviews.txt"))
        .query(query)
        .build();
  }

  @Override
  public void addReadBook(@NotNull String id) {
    try {
      GetResponse<BookResponse> response =
          esClient.get(g -> g.index("books").id(id), BookResponse.class);
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
