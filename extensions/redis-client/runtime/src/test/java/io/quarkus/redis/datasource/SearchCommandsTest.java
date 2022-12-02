package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.FieldOptions;
import io.quarkus.redis.datasource.search.FieldType;
import io.quarkus.redis.datasource.search.HighlightArgs;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.NumericFilter;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.SearchCommands;
import io.quarkus.redis.datasource.search.SpellCheckArgs;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Lots of tests are using await().untilAsserted as the indexing process runs in the background.
 */
@RequiresCommand("ft.create")
public class SearchCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private SearchCommands<String> search;
    private HashCommands<String, String, String> hash;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        search = ds.search();
        hash = ds.hash(String.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(search.getDataSource());
    }

    @Test
    void testCreationOfIndexAndSimpleSearches() {
        assertThat(search.ft_list()).isEmpty();
        search.ftCreate(key, new CreateArgs().indexedField("foo", FieldType.TEXT)
                .indexedField("test", "content", FieldType.TEXT)
                .indexedField("bar", FieldType.NUMERIC).payloadField("p"));
        assertThat(search.ft_list()).containsExactly(key);

        hash.hset("a", Map.of("foo", "hello world", "bar", "2", "test", "some text", "p", "a"));
        hash.hset("b", Map.of("foo", "hello monde", "bar", "3", "test", "some text", "p", "b"));
        hash.hset("c", Map.of("foo", "bonjour monde", "bar", "4", "test", "lorem ipsum", "p", "c"));

        var res = search.ftSearch(key, "hello");
        assertThat(res.count()).isEqualTo(2);
        assertThat(res.documents()).hasSize(2);
        assertThat(res.documents())
                .anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("a");
                    assertThat(d.property("foo").asString()).isEqualTo("hello world");
                    assertThat(d.property("bar").asInteger()).isEqualTo(2);
                    assertThat(d.property("test").asString()).isEqualTo("some text");
                }).anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("b");
                    assertThat(d.property("foo").asString()).isEqualTo("hello monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(3);
                    assertThat(d.property("test").asString()).isEqualTo("some text");
                });

        res = search.ftSearch(key, "(@bar:[3,4] | %ipsum%)", new QueryArgs().withScores());
        assertThat(res.count()).isEqualTo(2);
        assertThat(res.documents()).hasSize(2);
        assertThat(res.documents())
                .anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("b");
                    assertThat(d.score()).isEqualTo(1.0);
                    assertThat(d.property("foo").asString()).isEqualTo("hello monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(3);
                    assertThat(d.property("test").asString()).isEqualTo("some text");
                    assertThat(d.payload()).isNull();
                }).anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("c");
                    assertThat(d.score()).isEqualTo(3.0);
                    assertThat(d.property("foo").asString()).isEqualTo("bonjour monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(4);
                    assertThat(d.property("test").asString()).isEqualTo("lorem ipsum");
                    assertThat(d.payload()).isNull();
                });

        res = search.ftSearch(key, "(@bar:[3,4] | %ipsum%)", new QueryArgs().withScores().withPayloads());
        assertThat(res.count()).isEqualTo(2);
        assertThat(res.documents()).hasSize(2);
        assertThat(res.documents())
                .anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("b");
                    assertThat(d.score()).isEqualTo(1.0);
                    assertThat(d.property("foo").asString()).isEqualTo("hello monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(3);
                    assertThat(d.property("test").asString()).isEqualTo("some text");
                    assertThat(d.payload().toString()).isEqualTo("b");
                }).anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("c");
                    assertThat(d.score()).isEqualTo(3.0);
                    assertThat(d.property("foo").asString()).isEqualTo("bonjour monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(4);
                    assertThat(d.property("test").asString()).isEqualTo("lorem ipsum");
                    assertThat(d.payload().toString()).isEqualTo("c");
                });

        res = search.ftSearch(key, "(@bar:[3,4] | %ipsum%)", new QueryArgs().withScores().withPayloads().withSortKeys());
        assertThat(res.count()).isEqualTo(2);
        assertThat(res.documents()).hasSize(2);
        assertThat(res.documents())
                .anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("b");
                    assertThat(d.score()).isEqualTo(1.0);
                    assertThat(d.property("foo").asString()).isEqualTo("hello monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(3);
                    assertThat(d.property("test").asString()).isEqualTo("some text");
                    assertThat(d.payload().toString()).isEqualTo("b");
                }).anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("c");
                    assertThat(d.score()).isEqualTo(3.0);
                    assertThat(d.property("foo").asString()).isEqualTo("bonjour monde");
                    assertThat(d.property("bar").asInteger()).isEqualTo(4);
                    assertThat(d.property("test").asString()).isEqualTo("lorem ipsum");
                    assertThat(d.payload().toString()).isEqualTo("c");
                });

        res = search.ftSearch(key, "missing");
        assertThat(res.count()).isEqualTo(0);
        assertThat(res.documents()).isEmpty();

        res = search.ftSearch(key, "hello", new QueryArgs().returnAttribute("foo", "val"));
        assertThat(res.count()).isEqualTo(2);
        assertThat(res.documents()).hasSize(2);
        assertThat(res.documents())
                .anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("a");
                    assertThat(d.property("val").asString()).isEqualTo("hello world");
                    assertThat(d.property("bar")).isNull();
                    assertThat(d.property("test")).isNull();
                }).anySatisfy(d -> {
                    assertThat(d.key()).isEqualTo("b");
                    assertThat(d.property("val").asString()).isEqualTo("hello monde");
                    assertThat(d.property("bar")).isNull();
                    assertThat(d.property("test")).isNull();
                });

    }

    /**
     * Reproduce <a href="https://redis.io/docs/stack/search/quick_start/">Redis Search Quickstart</a>
     */
    @Test
    void testSearchQuickstart() {
        // > FT.CREATE myIdx ON HASH PREFIX 1 doc: SCHEMA title TEXT WEIGHT 5.0 body TEXT url TEXT
        search.ftCreate("myIdx", new CreateArgs().onHash().prefixes("doc")
                .indexedField("title", FieldType.TEXT, new FieldOptions().weight(5.0))
                .indexedField("body", FieldType.TEXT)
                .indexedField("url", FieldType.TEXT));
        // > HSET doc:1 title "hello world" body "lorem ipsum" url "http://redis.io"
        hash.hset("doc:1", Map.of("title", "hello world", "body", "lorem ipsum", "url", "https://redis.io"));

        // > FT.SEARCH myIdx "hello world" LIMIT 0 10
        var result = search.ftSearch("myIdx", "hello world", new QueryArgs().limit(0, 10));
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.documents().get(0).property("body").asString()).isEqualTo("lorem ipsum");

        // > FT.DROPINDEX myIdx
        search.ftDropIndex("myIdx", true);

        assertThat(hash.hget("doc:1", "title")).isNull();
        assertThat(search.ft_list()).isEmpty();

    }

    /**
     * Reproduce <a href="https://redis.io/docs/stack/search/indexing_json/">Indexing Json</a> - Part 1
     */
    @Test
    void testJsonIndexing() {
        // > FT.CREATE itemIdx ON JSON PREFIX 1 item: SCHEMA $.name AS name TEXT $.description as description TEXT $.price AS price NUMERIC
        search.ftCreate("itemIdx", new CreateArgs()
                .onJson()
                .prefixes("item:")
                .indexedField("$.name", "name", FieldType.TEXT)
                .indexedField("$.description", "description", FieldType.TEXT)
                .indexedField("$.price", "price", FieldType.NUMERIC));

        var json = ds.json();

        // > JSON.SET item:1 $ '{"name":"Noise-cancelling Bluetooth headphones",
        // "description":"Wireless Bluetooth headphones with noise-cancelling technology",
        // "connection":{"wireless":true,"type":"Bluetooth"},"price":99.98,"stock":25,"colors":["black","silver"]}'
        JsonObject json1 = JsonObject.of("name", "Noise-cancelling Bluetooth headphones",
                "description", "Wireless Bluetooth headphones with noise-cancelling technology",
                "connection", JsonObject.of("wireless", true, "type", "Bluetooth"),
                "price", 99.98, "stock", 25,
                "colors", JsonArray.of("black", "silver"));
        json.jsonSet("item:1", json1);
        // > JSON.SET item:2 $ '{"name":"Wireless earbuds","description":"Wireless Bluetooth in-ear headphones",
        // "connection":{"wireless":true,"type":"Bluetooth"},"price":64.99,"stock":17,"colors":["black","white"]}'
        JsonObject json2 = JsonObject.of("name", "Wireless earbuds", "description",
                "Wireless Bluetooth in-ear headphones",
                "connection", JsonObject.of("wireless", true, "type", "Bluetooth"),
                "price", 64.99, "stock", 17, "colors", JsonArray.of("black", "white"));
        json.jsonSet("item:2", json2);

        // > FT.SEARCH itemIdx '@name:(earbuds)'
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "@name:(earbuds)");
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.documents().get(0).key()).isEqualTo("item:2");
            assertThat(result.documents().get(0).property("$").asJsonObject()).isEqualTo(json2);
        });

        // > FT.SEARCH itemIdx '@description:(bluetooth headphones)'
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "@description:(bluetooth headphones)");
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents().get(0).key()).isEqualTo("item:1");
            assertThat(result.documents().get(0).property("$").asJsonObject()).isEqualTo(json1);
            assertThat(result.documents().get(1).key()).isEqualTo("item:2");
            assertThat(result.documents().get(1).property("$").asJsonObject()).isEqualTo(json2);
        });

        // >  FT.SEARCH itemIdx '@description:(bluetooth headphones) @price:[0 70]'
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "@description:(bluetooth headphones) @price:[0 70]");
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.documents().get(0).key()).isEqualTo("item:2");
            assertThat(result.documents().get(0).property("$").asJsonObject()).isEqualTo(json2);
        });

        // Part 2 - Tags
        // > > FT.CREATE itemIdx2 ON JSON PREFIX 1 item: SCHEMA $.colors.* AS colors TAG $.name AS name TEXT $.description as description TEXT
        search.ftCreate("itemIdx2", new CreateArgs().onJson()
                .prefixes("item:")
                .indexedField("$.colors.*", "colors", FieldType.TAG)
                .indexedField("$.name", "name", FieldType.TEXT)
                .indexedField("$.description", "description", FieldType.TEXT));

        // > FT.SEARCH itemIdx2 "@colors:{silver} (@name:(headphones)|@description:(headphones))"
        await().untilAsserted(() -> {
            var res = search.ftSearch("itemIdx2", "@colors:{silver} (@name:(headphones)|@description:(headphones))");
            assertThat(res.documents()).hasSize(1);
            assertThat(res.documents().get(0)).satisfies(doc -> {
                assertThat(doc.key()).isEqualTo("item:1");
                assertThat(doc.property("$").asJsonObject()).isEqualTo(json1);
            });
        });

        // Part 3 - Index JSON objects
        // > FT.CREATE itemIdx3 ON JSON SCHEMA $.connection.wireless AS wireless TAG $.connection.type AS connectionType TEXT
        search.ftCreate("itemIdx3", new CreateArgs().onJson().indexedField("$.connection.wireless", "wireless", FieldType.TAG)
                .indexedField("$.connection.type", "connectionType", FieldType.TEXT));
        // > FT.SEARCH itemIdx3 '@wireless:{true}'
        await().untilAsserted(() -> {
            var res = search.ftSearch("itemIdx3", "@wireless:{true}");
            assertThat(res.documents()).hasSize(2);
            assertThat(res.count()).isEqualTo(2);
        });

        // > FT.SEARCH itemIdx3 '@connectionType:(bluetooth)'
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx3", "@connectionType:(bluetooth)");
            assertThat(result.documents()).hasSize(2);
            assertThat(result.count()).isEqualTo(2);
        });

        // Part 4 - Field project
        // > FT.SEARCH itemIdx '@description:(headphones)' RETURN 2 name price
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "@description:(headphones)",
                    new QueryArgs().returnAttribute("name").returnAttribute("price"));
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).hasSize(2).allSatisfy(d -> {
                assertThat(d.property("name").asString()).isNotNull();
                assertThat(d.property("price").asDouble()).isGreaterThan(0);
            });
        });

        // >> FT.SEARCH itemIdx '@description:(headphones)' RETURN 3 name price $.stock
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "@description:(headphones)",
                    new QueryArgs().returnAttribute("name").returnAttribute("price").returnAttribute("$.stock"));
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).hasSize(2).allSatisfy(d -> {
                assertThat(d.property("name").asString()).isNotNull();
                assertThat(d.property("price").asDouble()).isGreaterThan(0);
                assertThat(d.property("$.stock").asDouble()).isGreaterThan(0);
            });
        });

        // >> FT.SEARCH itemIdx '@description:(headphones)' RETURN 3 name price $.stock AS stock
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "@description:(headphones)",
                    new QueryArgs().returnAttribute("name").returnAttribute("price").returnAttribute("$.stock", "stock"));
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).hasSize(2).allSatisfy(d -> {
                assertThat(d.property("name").asString()).isNotNull();
                assertThat(d.property("price").asDouble()).isGreaterThan(0);
                assertThat(d.property("stock").asDouble()).isGreaterThan(0);
            });
        });

        // > FT.SEARCH itemIdx '(@name:(bluetooth))|(@description:(bluetooth))' RETURN 3 name description price HIGHLIGHT FIELDS 2 name description TAGS '<b>' '</b>'
        await().untilAsserted(() -> {
            var result = search.ftSearch("itemIdx", "(@name:(bluetooth))|(@description:(bluetooth))", new QueryArgs()
                    .returnAttribute("name").returnAttribute("description").returnAttribute("price")
                    .highlight(new HighlightArgs().fields("name", "description").tags("<b>", "</b>")));
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).hasSize(2).allSatisfy(d -> {
                assertThat(d.property("name").asString()).isNotNull();
                assertThat(d.property("price").asDouble()).isGreaterThan(0);
                assertThat(d.property("description").asString()).contains("<b>Bluetooth</b>");
            });
        });

        // Part 4 - Aggregation
        // > FT.AGGREGATE itemIdx '*' LOAD 4 name $.price AS originalPrice APPLY '@originalPrice - (@originalPrice * 0.10)' AS salePrice SORTBY 2 @salePrice ASC
        await().untilAsserted(() -> {
            var r = search.ftAggregate("itemIdx", "*", new AggregateArgs().field("name").field("$.price", "originalPrice")
                    .apply(new AggregateArgs.Apply("@originalPrice - (@originalPrice * 0.10)", "salePrice"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@salePrice")));
            assertThat(r.documents().size()).isEqualTo(2);
            assertThat(r.documents()).hasSize(2).allSatisfy(d -> {
                assertThat(d.property("name").asString()).isNotNull();
                double price = d.property("originalPrice").asDouble();
                assertThat(price).isGreaterThan(0);
                assertThat(d.property("salePrice").asDouble()).isCloseTo(price - price * 0.10, Offset.offset(0.1));
            });
        });
    }

    void setupTraffic() {
        hash.hset("entry:1", Map.of("url", "about.html", "timestamp", Long.toString(System.currentTimeMillis() / 1000 - 10000),
                "country", "fr", "user_id", "user1"));
        hash.hset("entry:2", Map.of("url", "about.html", "timestamp", Long.toString(System.currentTimeMillis() / 1000 - 5000),
                "country", "de", "user_id", "user1"));
        hash.hset("entry:3", Map.of("url", "test.html", "timestamp", Long.toString(System.currentTimeMillis() / 1000 - 2000),
                "country", "fr", "user_id", "user2"));
        hash.hset("entry:4", Map.of("url", "about.html", "timestamp", Long.toString(System.currentTimeMillis() / 1000 - 4000),
                "country", "uk", "user_id", "user3"));
        hash.hset("entry:5", Map.of("url", "about.html", "timestamp", Long.toString(System.currentTimeMillis() / 1000 - 4000),
                "country", "fr", "user_id", "user4"));

        search.ftCreate("myIndex", new CreateArgs().prefixes("entry:")
                .indexedField("url", FieldType.TEXT, new FieldOptions().sortable())
                .indexedField("timestamp", FieldType.NUMERIC, new FieldOptions().sortable())
                .indexedField("country", FieldType.TAG, new FieldOptions().sortable())
                .indexedField("user_id", FieldType.TEXT, new FieldOptions().sortable().noIndex()));
    }

    /**
     * Reproduce the <a href="https://redis.io/docs/stack/search/reference/aggregations">Aggregation quickstart</a>
     */
    @Test
    void testAggregation() {
        setupTraffic();

        // > FT.AGGREGATE myIndex "*"
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "*", new AggregateArgs().allFields());
            assertThat(result.count()).isEqualTo(5);
        });

        // > FT.AGGREGATE myIndex "*" APPLY "@timestamp - (@timestamp % 3600)" AS hour
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "*", new AggregateArgs().allFields()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 3600)", "hour")));
            assertThat(result.count()).isEqualTo(5);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("hour").asDouble()).isPositive();
            });
        });

        // FT.AGGREGATE myIndex "*"
        //  APPLY "@timestamp - (@timestamp % 3600)" AS hour
        //  GROUPBY 1 @hour
        //  	REDUCE COUNT_DISTINCT 1 @user_id AS num_users
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "*", new AggregateArgs().allFields()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 3600)", "hour"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@hour").addReduceFunction("COUNT_DISTINCT", "num_users",
                            "@user_id"))
                    .verbatim());
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("hour").asDouble()).isPositive();
                assertThat(d.property("num_users").asInteger()).isPositive();
            });
        });

        // FT.AGGREGATE myIndex "*"
        //  APPLY "@timestamp - (@timestamp % 3600)" AS hour
        //  GROUPBY 1 @hour
        //  	REDUCE COUNT_DISTINCT 1 @user_id AS num_users
        //  SORTBY 2 @hour ASC
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "*", new AggregateArgs().allFields()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 3600)", "hour"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@hour").addReduceFunction("COUNT_DISTINCT", "num_users",
                            "@user_id"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@hour")));
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("hour").asDouble()).isPositive();
                assertThat(d.property("num_users").asInteger()).isPositive();
            });
        });

        // FT.AGGREGATE myIndex "*"
        //  APPLY "@timestamp - (@timestamp % 3600)" AS hour
        //  GROUPBY 1 @hour
        //  	REDUCE COUNT_DISTINCT 1 @user_id AS num_users
        //  SORTBY 2 @hour ASC
        //  APPLY timefmt(@hour) AS hour
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "*", new AggregateArgs().allFields()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 3600)", "hour"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@hour").addReduceFunction("COUNT_DISTINCT", "num_users",
                            "@user_id"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@hour"))
                    .apply(new AggregateArgs.Apply("timefmt(@hour)", "hour")));
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("hour").asString()).isNotBlank();
                assertThat(d.property("num_users").asInteger()).isPositive();
            });
        });

        // FT.AGGREGATE myIndex "@url:\"about.html\""
        //    APPLY "@timestamp - (@timestamp % 86400)" AS day
        //    GROUPBY 2 @day @country
        //    	REDUCE count 0 AS num_visits
        //    SORTBY 4 @day ASC @country DESC
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "@url:\"about.html\"", new AggregateArgs()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 86400)", "day"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@day").addProperty("@country").addReduceFunction("count",
                            "num_visits"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@day").descending("@country")));
            assertThat(result.count()).isEqualTo(3);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("day").asInteger()).isPositive();
                assertThat(d.property("country").asString()).isNotNull();
                assertThat(d.property("num_visits").asInteger()).isPositive();
            });
        });

        // EXTRA - Filters
        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "@url:\"about.html\"", new AggregateArgs()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 86400)", "day"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@day").addProperty("@country").addReduceFunction("count",
                            "num_visits"))
                    .filter("@country=='fr'")
                    .sortBy(new AggregateArgs.SortBy().ascending("@day").descending("@country"))
                    .filter("@num_visits>=1"));
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("day").asInteger()).isPositive();
                assertThat(d.property("country").asString()).isEqualTo("fr");
                assertThat(d.property("num_visits").asInteger()).isPositive();
            });
        });

        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "@url:\"about.html\"", new AggregateArgs()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 86400)", "day"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@day").addProperty("@country").addReduceFunction("count",
                            "num_visits"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@day").descending("@country"))
                    .withCursor().cursorCount(2));
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("day").asInteger()).isPositive();
                assertThat(d.property("country").asString()).isNotNull();
                assertThat(d.property("num_visits").asInteger()).isPositive();
            });
            assertThat(result.cursor()).isPositive();
            result = search.ftCursorRead("myIndex", result.cursor());
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("day").asInteger()).isPositive();
                assertThat(d.property("country").asString()).isNotNull();
                assertThat(d.property("num_visits").asInteger()).isPositive();
            });
            assertThat(result.cursor()).isEqualTo(0);
        });

        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "@url:\"about.html\"", new AggregateArgs()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 86400)", "day"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@day").addProperty("@country").addReduceFunction("count",
                            "num_visits"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@day").descending("@country"))
                    .withCursor().cursorCount(10));
            assertThat(result.count()).isEqualTo(3);
            assertThat(result.cursor()).isEqualTo(0);
        });

        await().untilAsserted(() -> {
            var result = search.ftAggregate("myIndex", "@url:\"about.html\"", new AggregateArgs()
                    .apply(new AggregateArgs.Apply("@timestamp - (@timestamp % 86400)", "day"))
                    .groupBy(new AggregateArgs.GroupBy().addProperty("@day").addProperty("@country").addReduceFunction("count",
                            "num_visits"))
                    .sortBy(new AggregateArgs.SortBy().ascending("@day").descending("@country"))
                    .withCursor().cursorCount(1));
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.cursor()).isPositive();
            var c = result.cursor();
            search.ftCursorDel("myIndex", result.cursor());
            assertThatThrownBy(() -> {
                search.ftCursorRead("myIndex", c);
            }).hasMessageContaining("Cursor not found");
        });
    }

    /**
     * Reproduce <a href="https://developer.redis.com/howtos/moviesdatabase">the Movie Database example</a>
     */
    @Test
    void testMovies() throws InterruptedException {
        setupMovies();

        // FT.SEARCH idx:movie "war"
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "war");
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).allSatisfy(d -> assertThat(d.property("title").asString()).contains("Star Wars"));
        });

        // FT.SEARCH idx:movie "war" RETURN 2 title release_year
        QueryArgs args = new QueryArgs().returnAttribute("title").returnAttribute("release_year", "year");

        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "war", args);
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("title").asString()).contains("Star Wars");
                assertThat(d.property("year").asInteger()).isIn(1972, 1983);
            });
        });
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "@title:war", args);
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("title").asString()).contains("Star Wars");
                assertThat(d.property("year").asInteger()).isIn(1972, 1983);
            });
        });

        // > FT.SEARCH idx:movie "war -jedi" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "war -jedi", args);
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("title").asString()).contains("Star Wars");
                assertThat(d.property("year").asInteger()).isIn(1972);
            });
        });

        // > FT.SEARCH idx:movie " %gdfather% " RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "%gdfather%", args);
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("title").asString()).contains("The Godfather");
                assertThat(d.property("year").asInteger()).isIn(1972);
            });
        });

        // > FT.SEARCH idx:movie "@genre:{Thriller}" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "@genre:{Thriller}", args);
            assertThat(result.count()).isEqualTo(1);
            assertThat(result.documents()).allSatisfy(d -> {
                assertThat(d.property("title").asString()).contains("Heat");
                assertThat(d.property("year").asInteger()).isIn(1995);
            });
        });

        // FT.SEARCH idx:movie "@genre:{Thriller|Action}" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "@genre:{Thriller|Action}", args);
            assertThat(result.count()).isEqualTo(3);
        });

        // > FT.SEARCH idx:movie "@genre:{Thriller|Action} @title:-jedi" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "@genre:{Thriller|Action} @title:-jedi", args);
            assertThat(result.count()).isEqualTo(2);
        });

        // > FT.SEARCH idx:movie * FILTER release_year 1970 1980 RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "*", new QueryArgs()
                    .filter(new NumericFilter("release_year", 1970, 1980)));
            assertThat(result.count()).isEqualTo(2);
        });

        // > FT.SEARCH idx:movie "@release_year:[1970 (1980]" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "@release_year:[1970 (1980]");
            assertThat(result.count()).isEqualTo(2);
        });

        // > FT.SEARCH idx:movie "*" LIMIT 0 0
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "*", new QueryArgs().limit(0, 0));
            assertThat(result.count()).isEqualTo(4);
            assertThat(result.documents()).isEmpty();
        });

        hash.hset("movie:11033", Map.of("title", "Tomorrow Never Dies",
                "plot", "James Bond sets out to stop a media mogul's plan to...",
                "release_year", "1997",
                "genre", "Action",
                "rating", "6.5",
                "votes", "177732",
                "imbd_id", "tt0120347"));

        // > FT.SEARCH idx:movie "never" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "never");
            assertThat(result.count()).isEqualTo(1);
        });

        // > HSET movie:11033 title "Tomorrow Never Dies - 007"
        hash.hset("movie:11033", "title", "Tomorrow Never Dies - 007");
        //> FT.SEARCH idx:movie "007" RETURN 2 title release_year
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "007");
            assertThat(result.count()).isEqualTo(1);
        });

        // > EXPIRE "movie:11033"
        ds.key().pexpire("movie:11033", 10);
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "007");
            assertThat(result.count()).isEqualTo(0);
        });

        // > FT._LIST
        await().untilAsserted(() -> {
            assertThat(search.ft_list()).containsExactly("idx:movie");
        });

        // > FT.ALTER idx:movie SCHEMA ADD plot TEXT WEIGHT 0.5
        search.ftAlter("idx:movie", IndexedField.from("plot", FieldType.TEXT, new FieldOptions().weight(0.5)));

        // > FT.SEARCH idx:movie "empire @genre:{Action}"
        await().untilAsserted(() -> {
            var result = search.ftSearch("idx:movie", "empire @genre:{Action}");
            assertThat(result.count()).isEqualTo(2);
            assertThat(result.documents().get(0).property("title").asString())
                    .isEqualTo("Star Wars: Episode V - The Empire Strikes Back");
        });

        // FT.DROPINDEX idx:movie
        search.ftDropIndex("idx:movie");
        assertThat(search.ft_list()).isEmpty();

    }

    @Test
    void testAliases() {
        setupMovies();

        search.ftAliasAdd("my-index", "idx:movie");

        assertThatThrownBy(() -> search.ftAliasAdd("another", "missing"));

        QueryArgs args = new QueryArgs().returnAttribute("title").returnAttribute("release_year", "year");
        var result = search.ftSearch("my-index", "war", args);
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.documents()).allSatisfy(d -> {
            assertThat(d.property("title").asString()).contains("Star Wars");
            assertThat(d.property("year").asInteger()).isIn(1972, 1983);
        });

        setupTraffic();
        search.ftAliasUpdate("my-index", "myIndex");

        assertThatThrownBy(() -> search.ftAliasAdd("my-index", "idx:movie"));

        await().untilAsserted(() -> {
            var res = search.ftAggregate("my-index", "*", new AggregateArgs().allFields());
            assertThat(res.count()).isEqualTo(5);
        });

        search.ftAliasDel("my-index");
        assertThatThrownBy(() -> search.ftAggregate("my-index", "*", new AggregateArgs().allFields()));
    }

    @Test
    void testAlterSchema() {
        setupMovies();
        assertThatThrownBy(() -> search.ftAlter("missing", IndexedField.from("test", FieldType.TEXT)));
        search.ftAlter("idx:movie", IndexedField.from("plot", FieldType.TEXT, new FieldOptions().weight(0.5)));
        await()
                .untilAsserted(() -> assertThat(search.ftSearch("idx:movie", "brutally").count()).isEqualTo(1));
    }

    @Test
    void testTagVals() {
        setupMovies();
        assertThat(search.ftTagVals("idx:movie", "genre")).containsExactlyInAnyOrder("drama", "action", "thriller");
        assertThatThrownBy(() -> search.ftTagVals("idx:movie", "title"));
    }

    @Test
    void testDictAndSpellcheck() {
        search.ftCreate("my-index", new CreateArgs().prefixes("word:").indexedField("word", FieldType.TEXT));
        hash.hset("word:1", "word", "hockey");
        hash.hset("word:2", "word", "stick");
        hash.hset("word:3", "word", "stir");
        var res = search.ftSpellCheck("my-index", "Hockye stik");
        assertThat(res.misspelledWords()).containsExactly("hockye", "stik");
        assertThat(res.suggestions("hockye")).hasSize(1).allSatisfy(sug -> {
            assertThat(sug.distance()).isPositive();
            assertThat(sug.word()).isEqualTo("hockey");
        });
        assertThat(res.suggestions("stik")).hasSize(2).allSatisfy(sug -> {
            assertThat(sug.distance()).isPositive();
            assertThat(sug.word()).isIn("stir", "stick");
        });
        assertThat(search.ftSpellCheck("my-index", "hockey Stick").isCorrect()).isTrue();

        search.ftDictAdd("my-dict", "hello", "bonjour", "magic", "supercalifragilisticexpialidocious");
        assertThat(search.ftDictDump("my-dict")).hasSize(4);
        search.ftDictAdd("my-dict", "another");
        assertThat(search.ftDictDump("my-dict")).hasSize(5);
        assertThat(search.ftSpellCheck("my-index", "bonjour hockey", new SpellCheckArgs().includes("my-dict")).isCorrect())
                .isTrue();
        assertThat(
                search.ftSpellCheck("my-index", "bonjour hockey", new SpellCheckArgs().includes("my-dict")).misspelledWords())
                .isEmpty();

        res = search.ftSpellCheck("my-index", "bonjour magyc hocky", new SpellCheckArgs().includes("my-dict").distance(3));
        assertThat(res.suggestions("bonjour")).isNull();
        assertThat(res.suggestions("magyc")).hasSize(1).allSatisfy(su -> {
            assertThat(su.word()).isEqualTo("magic");
        });
        assertThat(res.suggestions("hocky")).hasSize(1).allSatisfy(su -> {
            assertThat(su.word()).isEqualTo("hockey");
            assertThat(su.distance()).isPositive();
        });

        search.ftDictDel("my-dict", "bonjour");
        res = search.ftSpellCheck("my-index", "bonjour magyc hocky", new SpellCheckArgs().includes("my-dict").distance(3));
        assertThat(res.misspelledWords()).containsExactly("bonjour", "magyc", "hocky");
        assertThat(res.suggestions("bonjour")).isEmpty();
        assertThat(res.suggestions("magyc")).hasSize(1).allSatisfy(su -> {
            assertThat(su.word()).isEqualTo("magic");
        });
        assertThat(res.suggestions("hocky")).hasSize(1).allSatisfy(su -> {
            assertThat(su.word()).isEqualTo("hockey");
            assertThat(su.distance()).isPositive();
        });

    }

    void setupMovies() {
        // FT.CREATE idx:movie ON hash PREFIX 1 "movie:" SCHEMA title TEXT SORTABLE release_year NUMERIC SORTABLE rating NUMERIC SORTABLE genre TAG SORTABLE
        search.ftCreate("idx:movie", new CreateArgs().onHash().prefixes("movie:")
                .indexedField("title", FieldType.TEXT, new FieldOptions().sortable())
                .indexedField("release_year", FieldType.NUMERIC, new FieldOptions().sortable())
                .indexedField("rating", FieldType.NUMERIC, new FieldOptions().sortable())
                .indexedField("genre", FieldType.TAG, new FieldOptions().sortable()));

        hash.hset("movie:11002", Map.of("title", "Star Wars: Episode V - The Empire Strikes Back",
                "plot", "After the Rebels are brutally overpowered by the Empire on the ice planet Hoth, ...",
                "release_year", "1972",
                "genre", "Action",
                "rating", "8.7",
                "votes", "1127635",
                "imbd_id", "tt0080684"));
        hash.hset("movie:11003", Map.of("title", "The Godfather",
                "plot", "The aging patriarch of an organized crime dynasty transfers control of his ...",
                "release_year", "1972",
                "genre", "Drama",
                "rating", "9.2",
                "votes", "1563839",
                "imbd_id", "tt0068646"));
        hash.hset("movie:11004", Map.of("title", "Heat",
                "plot", "A group of professional bank robbers start to feel the heat ...",
                "release_year", "1995",
                "genre", "Thriller",
                "rating", "8.2",
                "votes", "559490",
                "imbd_id", "tt0113277"));
        hash.hset("movie:11005", Map.of("title", "Star Wars: Episode VI - Return of the Jedi",
                "plot", "The Rebels dispatch to Endor to destroy the second Empire's Death Star.",
                "release_year", "1983",
                "genre", "Action",
                "rating", "8.3",
                "votes", "906260",
                "imbd_id", "tt0086190"));
    }

    @Test
    void testSynonyms() {
        search.ftCreate("my-index", new CreateArgs().prefixes("word:").indexedField("word", FieldType.TEXT));
        search.ftSynUpdate("my-index", "hello", "bonjour", "ola", "hey");
        search.ftSynUpdate("my-index", "bye", "ciao", "au revoir", "hey");

        var resp = search.ftSynDump("my-index");
        assertThat(resp.groups()).contains("hello", "bye");
        assertThat(resp.synonym("hello")).containsExactlyInAnyOrder("bonjour", "ola", "hey");
        assertThat(resp.synonym("bye")).containsExactlyInAnyOrder("ciao", "au revoir", "hey");
    }

    @Test
    void testSynonymsInQueries() {
        search.ftCreate("idx", new CreateArgs().indexedField("t", FieldType.TEXT));
        search.ftSynUpdate("idx", "group1", "hello", "world");
        hash.hset("foo", "t", "hello");
        hash.hset("bar", "t", "world");

        var res = search.ftSearch("idx", "hello");
        assertThat(res.count()).isEqualTo(2);
        assertThat(res.documents()).anySatisfy(d -> assertThat(d.property("t").asString()).isEqualTo("hello"));
        assertThat(res.documents()).anySatisfy(d -> assertThat(d.property("t").asString()).isEqualTo("world"));
    }
}
