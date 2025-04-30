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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class BookEngine implements SearchEngine {
  String serverUrl = "http://localhost:9200"; // todo: why does it need ot be http and not https?
  //    String apiKey = "";
  ElasticsearchClient esClient;

  // List of Pairs, first component being the ID and second component being the book information
  List<Pair<String, BookResponse>> cachedReadBooks = new ArrayList<>();

  HashMap<String, Double> genrePreferences = new HashMap<>();
  HashMap<String, Double> authorPreferences = new HashMap<>();

  public BookEngine() {
    esClient =
        ElasticsearchClient.of(
            b -> b.host(serverUrl).usernameAndPassword("elastic", "gg7onaIm")
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

      computeUserPreferences();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void computeUserPreferences() {
    genrePreferences = new HashMap<>();
    authorPreferences = new HashMap<>();

    if (cachedReadBooks.isEmpty()) {
      return;
    }

    // count genres and authors
    for (Pair<String, BookResponse> bookPair : cachedReadBooks) {
      BookResponse book = bookPair.component2();

      // process genres
      if (book.genres() != null) {
        for (String genre : book.genres()) {
          genrePreferences.put(genre, genrePreferences.getOrDefault(genre, 0.0) + 1.0);
        }
      }

      // process author
      if (book.author() != null) {
        authorPreferences.put(
            book.author(), authorPreferences.getOrDefault(book.author(), 0.0) + 1.0);
      }

      // Normalize preferences
      normalizePreferences(genrePreferences);
      normalizePreferences(authorPreferences);
    }
  }

  private void normalizePreferences(HashMap<String, Double> preferences) {
    if (preferences.isEmpty()) {
      return;
    }

    // Find the maximum value
    double maxValue = preferences.values().stream().max(Double::compare).orElse(1.0);

    // Normalize all values
    for (String key : new HashSet<>(preferences.keySet())) {
      preferences.put(key, preferences.get(key) / maxValue);
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

      // more like this query
      if (!cachedReadBooks.isEmpty()) {
        MoreLikeThisQuery moreLikeThisQuery = buildMoreLikeThisQuery(cachedReadBooks);
        booleanQueryBuilder.should(moreLikeThisQuery._toQuery());
      }

      // complete query
      Query searchQuery = new Query.Builder().bool(booleanQueryBuilder.build()).build();

      // search request
      SearchResponse<BookResponse> searchRequest =
          esClient.search(builder -> builder.index("books").query(searchQuery), BookResponse.class);

      // process query and return result
      return queryResult(searchRequest);

      //            SearchResponse<BookResponse> searchResult = esClient.search(s -> s
      //                            .index("books")
      //                            .query(q -> q
      //                                    .bool(bq -> {
      //                                        bq.should(q1 -> q1
      //                                                // basic query
      //                                                .simpleQueryString(sqs -> sqs
      //                                                        .fields(List.of("author",
      // "description", "genres", "title"))
      //                                                        .query(query)));
      //                                        // if the list is empty the query crashes
      //                                        if (!cachedReadBooks.isEmpty()) {
      //                                            bq.should(q2 -> q2
      //                                                    .moreLikeThis(mls -> mls
      //                                                            .fields(List.of("author",
      // "genres"))
      //                                                            // turns the cachedReadBooks
      // into a list of ids which is run with the morelikethis query
      //                                                            .like(cachedReadBooks.stream()
      //                                                                    .map(cb -> Like
      //                                                                            .of(l -> l
      //                                                                            .document(ld ->
      // ld
      //
      // .index("books")
      //
      // .id(cb.component1()))))
      //                                                                    .toList())
      //                                                            .boost(0.5F)));
      //                                        }
      //                                        return bq;
      //                                    })
      //                            ),
      //                    BookResponse.class);
      //            return relevanceFeedback(searchResult);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return a list of books to recommend to the user
   *
   * @param searchRequest the elastic search request
   * @return a list of books
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
   * @param readBooks list of books the user has read
   * @return a more like this query
   */
  private MoreLikeThisQuery buildMoreLikeThisQuery(List<Pair<String, BookResponse>> readBooks) {
    // Create a list of document references for books the user has read
    List<Like> likeDocuments =
        readBooks.stream()
            .map(
                book ->
                    Like.of(
                        l ->
                            l.document(
                                new LikeDocument.Builder()
                                    .index("books")
                                    .id(book.component1())
                                    .build())))
            .toList();

    // Build and return the more_like_this query
    return new MoreLikeThisQuery.Builder()
        .fields(List.of("author", "genres"))
        .like(likeDocuments)
        .boost(0.5F) // weight given to personalization
        .minTermFreq(1) // include terms that appear at least once
        .maxQueryTerms(12) // get top terms to influence the search
        .minimumShouldMatch("30%") // results contain at least 30% of the query terms
        .build();
  }

  /**
   * Construct an elastic search simplequerstring query
   *
   * @param query request by user
   * @return elasticsearch simplequerystringquery
   */
  private SimpleQueryStringQuery buildSimpleStringQuery(@NotNull String query) {
    return new SimpleQueryStringQuery.Builder()
        .fields(List.of("author", "description", "genres", "title"))
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
