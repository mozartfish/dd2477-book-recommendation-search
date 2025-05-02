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

  // user preference data - store frequency information
  HashMap<String, Double> genrePreferences = new HashMap<>();
  HashMap<String, Double> authorPreferences = new HashMap<>();

  // boosting factors
  public static final float CONTENT_WEIGHT = 0.4F;
  public static final float USER_PREFERENCES_WEIGHT = 0.6F;
  public static final float LESS_FREQUENTED_GENRES = 0.3F;
  public static final float RATING_COUNT_WEIGHT = 0.2F;

  public BookEngine() {
    esClient =
        ElasticsearchClient.of(
            b -> b.host(serverUrl).usernameAndPassword("elastic", "mWQ787fk")
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

      // compute the user preferences based on the profile upload
      computeUserPreferences();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Count and store user favorite authors and genres */
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

      // process authors
      if (book.author() != null) {
        authorPreferences.put(
            book.author(), authorPreferences.getOrDefault(book.author(), 0.0) + 1.0);
      }

      // normalize preferences
      normalizePreferences(genrePreferences);
      normalizePreferences(authorPreferences);
    }
  }

  /**
   * Map preference scores to 0.0 - 1.0
   *
   * @param preferences
   */
  private void normalizePreferences(HashMap<String, Double> preferences) {
    if (preferences.isEmpty()) {
      return;
    }

    // find max value in the records
    double maxValue = preferences.values().stream().max(Double::compare).orElse(1.0);

    // normalize all values
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

      // Content-Based Filtering
      // boolean query builder
      BoolQuery.Builder booleanQueryBuilder = new BoolQuery.Builder();

      // simple query
      SimpleQueryStringQuery simpleQuery = buildSimpleStringQuery(query);
      booleanQueryBuilder.should(simpleQuery._toQuery());

      // author and genre preferences
      if (!cachedReadBooks.isEmpty()
          && (!genrePreferences.isEmpty() || !authorPreferences.isEmpty())) {
        Query preferenceBoostQuery = buildPreferenceBoostQuery();
        booleanQueryBuilder.should(preferenceBoostQuery);
      }

      // boost less frequented genres
      Query lessFrequentedGenreQuery = buildHiddenGenresQuery();
      booleanQueryBuilder.should(lessFrequentedGenreQuery);

      // Collaborative Filtering
      // more like this query
      if (!cachedReadBooks.isEmpty()) {
        MoreLikeThisQuery moreLikeThisQuery = buildMoreLikeThisQuery(cachedReadBooks);
        booleanQueryBuilder.should(moreLikeThisQuery._toQuery());
      }

      // popularity - based on the number of reviews (review count field)
      Query reviewPopularityQuery = buildPopularityBoostQuery();
      booleanQueryBuilder.should(reviewPopularityQuery);

      // complete query
      Query searchQuery = new Query.Builder().bool(booleanQueryBuilder.build()).build();
      //
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
   * Query that boosts genres that the user has read less frequently
   *
   * @return
   */
  private Query buildHiddenGenresQuery() {
    HashMap<String, Double> lessFrequentedGenres = new HashMap<>();

    // Find genres that are less frequently read
    genrePreferences.entrySet().stream()
        .filter(entry -> entry.getValue() > 0.1 && entry.getValue() < 0.4)
        .forEach(entry -> lessFrequentedGenres.put(entry.getKey(), 0.3));

    BoolQuery.Builder diversityBuilder = new BoolQuery.Builder();

    // boost each genre
    for (HashMap.Entry<String, Double> entry : lessFrequentedGenres.entrySet()) {
      diversityBuilder.should(q -> q.
              match(m -> m.
                      field("genres").query(entry.getKey()).boost(entry.getValue().floatValue())))
              .boost(LESS_FREQUENTED_GENRES);
    }

    return new Query.Builder().bool(diversityBuilder.build()).build();
  }

  /**
   * Construct a query that boosts books based on the rating count - the more ratings left, the more
   * popular????
   *
   * @return query that uses rating count
   */
  private Query buildPopularityBoostQuery() {
    return new Query.Builder()
        .functionScore(
            fs ->
                fs.query(q -> q.matchAll(m -> m))
                    .functions(
                        f ->
                            f.fieldValueFactor(
                                fvf ->
                                    fvf.field("ratingsCount") // rating count for popularity
                                        .modifier(FieldValueFactorModifier.Log1p) // log scaling
                                        .factor((double) RATING_COUNT_WEIGHT))))
        .build();
  }

  /**
   * Query that boosts results based on users favorite genres and authors
   *
   * @return query that uses favorite genres and authors
   */
  private Query buildPreferenceBoostQuery() {
    BoolQuery.Builder preferenceBuilder = new BoolQuery.Builder();

    // genre boosting query
    genrePreferences.entrySet().stream()
        .filter(entry -> entry.getValue() > 0.2) // Only use significant preferences
        .forEach(
            entry -> {
              preferenceBuilder.should(
                  new Query.Builder()
                      .match(
                          m ->
                              m.field("genres")
                                  .query(entry.getKey())
                                  .boost(entry.getValue().floatValue()))
                      .build());
            });

    // authors
    authorPreferences.entrySet().stream()
        .filter(entry -> entry.getValue() > 0.2)
        .forEach(
            entry -> {
              preferenceBuilder.should(
                  new Query.Builder()
                      .match(
                          m ->
                              m.field("author")
                                  .query(entry.getKey())
                                  .boost(entry.getValue().floatValue()))
                      .build());
            });

    return new Query.Builder().bool(preferenceBuilder.build()).build();
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
        .fields(List.of("author", "genres", "description"))
        .like(likeDocuments)
        .boost(USER_PREFERENCES_WEIGHT) // weight given to personalization
        .minTermFreq(1) // include terms that appear at least once
        .maxQueryTerms(15) // get top terms to influence the search
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
        .query(query).boost(BookEngine.CONTENT_WEIGHT)
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
