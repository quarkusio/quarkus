
### Setup

Requires `hibernate-search-orm-elasticsearch` + `hibernate-orm` (or `hibernate-orm-panache`) + a JDBC driver.

**Mandatory configuration** — set the Elasticsearch version to match Dev Services:

```properties
quarkus.hibernate-search-orm.elasticsearch.version=9
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
%prod.quarkus.hibernate-orm.schema-management.strategy=update
```

Dev Services starts Elasticsearch automatically. The version property must match — check the Dev Services log for the actual version started.

### Indexing Entities

Annotations are from `org.hibernate.search.mapper.pojo.mapping.definition.annotation`.

```java
@Entity
@Indexed
public class Article {

    @Id
    @GeneratedValue
    public Long id;

    @FullTextField(analyzer = "standard")
    public String title;

    @FullTextField(analyzer = "standard")
    public String content;

    @KeywordField
    public String author;

    @GenericField(sortable = Sortable.YES)
    public LocalDate publishedAt;
}
```

- `@Indexed` — marks the entity for indexing in Elasticsearch.
- `@FullTextField` — full-text search with analysis (tokenized, lowercased). Specify `analyzer`.
- `@KeywordField` — exact match, no analysis. Use for filters, facets (`aggregable = Aggregable.YES`), sorting (`sortable = Sortable.YES`).
- `@GenericField` — for non-text types (dates, numbers). Add `sortable = Sortable.YES` for sort fields.

### Searching

Inject `SearchSession` and use the search DSL:

```java
@Inject SearchSession searchSession;

@Transactional
public List<Article> search(String query, String author) {
    return searchSession.search(Article.class)
        .where(f -> {
            var bool = f.bool();
            bool.must(f.match().fields("title", "content").matching(query));
            if (author != null) {
                bool.filter(f.match().field("author").matching(author));
            }
            return bool;
        })
        .sort(f -> f.field("publishedAt").desc())
        .fetchHits(20);
}
```

- `f.match().fields("title", "content").matching(query)` — full-text match across multiple fields.
- `f.bool()` — combine predicates with `must`, `should`, `filter`, `mustNot`. `must` affects relevance scoring; `filter` does not (use for yes/no constraints). For `should` clauses, use `minimumShouldMatch(n)` to require at least `n` matches.
- `f.range().field("price").between(min, max)` — range queries.
- `f.matchAll()` — match all documents.
- `f.simpleQueryString().fields(...).matching(query)` — user-facing query syntax (supports `+`, `-`, `"`).
- `.fetchHits(maxResults)` returns `List<Entity>`. `.fetchAllHits()` returns all. `.fetchTotalHitCount()` returns count only.

See the [Hibernate Search documentation](https://docs.jboss.org/hibernate/search/7.2/reference/en-US/html_single/#search-dsl-predicate) for the full list of predicates.

### Mass Indexing

Re-index all data from the database to Elasticsearch:

```java
@Transactional
public void reindex() throws InterruptedException {
    searchSession.massIndexer(Article.class).startAndWait();
}
```

Call this after bulk data loads or schema changes. Runs in the background by default.

### Custom Analyzers

Define analyzers via `ElasticsearchAnalysisConfigurer`:

```java
@SearchExtension
public class MyAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        context.analyzer("english").custom()
            .tokenizer("standard")
            .tokenFilters("lowercase", "porter_stem");
        context.normalizer("sort").custom()
            .tokenFilters("lowercase", "asciifolding");
    }
}
```

Reference on the indexed field: `@FullTextField(analyzer = "english")`, `@KeywordField(normalizer = "sort")`.

### Sorting

```java
@KeywordField(name = "title_sort", sortable = Sortable.YES, normalizer = "sort")
public String title;

// In search:
.sort(f -> f.field("title_sort").asc())
```

`@FullTextField` is NOT sortable — add a separate `@KeywordField` with `sortable = YES` and a normalizer for sort fields.

### Testing

- Dev Services starts Elasticsearch automatically — no manual setup.
- Use `@Transactional` on search methods in tests (SearchSession requires an active session).
- Set `quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=sync` in test config to ensure indexes are fully updated before querying.
- For `massIndexer`, call `.startAndWait()` to block until complete.

### Common Pitfalls

- `quarkus.hibernate-search-orm.elasticsearch.version` is **mandatory** — the app won't start without it. Set it to match the Dev Services Elasticsearch version.
- `@FullTextField` cannot be used for sorting or filtering — use `@KeywordField` for exact match/filter/sort.
- Search methods need `@Transactional` or an active Hibernate ORM session context.
- Build-time config changes (like `elasticsearch.version`) require a full restart — hot reload doesn't apply.
- `fetchHits(n)` returns at most `n` results — use `fetchAllHits()` only for small result sets.
