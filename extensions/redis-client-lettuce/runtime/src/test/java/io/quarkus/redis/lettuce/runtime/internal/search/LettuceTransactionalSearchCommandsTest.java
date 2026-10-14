package io.quarkus.redis.lettuce.runtime.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.FieldOptions;
import io.quarkus.redis.datasource.search.FieldType;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.search.SearchQueryResponse;
import io.quarkus.redis.datasource.search.TransactionalSearchCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;

@SuppressWarnings("ConstantConditions")
class LettuceTransactionalSearchCommandsTest extends CommandsTestBase {

    RedisDataSource blockingDs;
    ReactiveRedisDataSource reactiveDs;
    HashCommands<String, String, String> hash;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource(Duration.ofSeconds(60));
        hash = blockingDs.hash(String.class);
    }

    void setup() {
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
    void transactionalSearchBlocking() {
        setup();

        TransactionResult result = blockingDs.withTransaction(tx -> {
            TransactionalSearchCommands search = tx.search();
            assertThat(search.getDataSource()).isEqualTo(tx);
            search.ftCreate("idx:movie", new CreateArgs().onHash().prefixes("movie:")
                    .indexedField("title", FieldType.TEXT, new FieldOptions().sortable())
                    .indexedField("release_year", FieldType.NUMERIC, new FieldOptions().sortable())
                    .indexedField("rating", FieldType.NUMERIC, new FieldOptions().sortable())
                    .indexedField("genre", FieldType.TAG, new FieldOptions().sortable()));
            search.ftSearch("idx:movie", "war");
            // Lettuce only accepts whole-number weights, the Vert.x test uses 0.5 here
            search.ftAlter("idx:movie", IndexedField.from("plot", FieldType.TEXT, new FieldOptions().weight(2.0)));
            search.ftSearch("idx:movie", "empire @genre:{Action}", new QueryArgs().returnAttribute("title"));
        });

        assertTransactionalSearch(result);
    }

    @Test
    void transactionalSearchReactive() {
        setup();

        TransactionResult result = reactiveDs.withTransaction(tx -> {
            ReactiveTransactionalSearchCommands search = tx.search();
            assertThat(search.getDataSource()).isEqualTo(tx);
            var u1 = search.ftCreate("idx:movie", new CreateArgs().onHash().prefixes("movie:")
                    .indexedField("title", FieldType.TEXT, new FieldOptions().sortable())
                    .indexedField("release_year", FieldType.NUMERIC, new FieldOptions().sortable())
                    .indexedField("rating", FieldType.NUMERIC, new FieldOptions().sortable())
                    .indexedField("genre", FieldType.TAG, new FieldOptions().sortable()));
            var u2 = search.ftSearch("idx:movie", "war");
            var u3 = search.ftAlter("idx:movie", IndexedField.from("plot", FieldType.TEXT, new FieldOptions().weight(2.0)));
            var u4 = search.ftSearch("idx:movie", "empire @genre:{Action}", new QueryArgs().returnAttribute("title"));

            return u1.chain(() -> u2).chain(() -> u3).chain(() -> u4);
        }).await().indefinitely();

        assertTransactionalSearch(result);
    }

    private void assertTransactionalSearch(TransactionResult result) {
        assertThat(result.size()).isEqualTo(4);
        assertThat(result.discarded()).isFalse();
        assertThat(result.hasErrors()).isFalse();
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1)).isInstanceOf(SearchQueryResponse.class);
        assertThat((Object) result.get(2)).isNull();
        assertThat((Object) result.get(3)).isInstanceOf(SearchQueryResponse.class);

        // The index created and altered by the transaction is usable afterwards
        await().untilAsserted(() -> {
            var war = blockingDs.search().ftSearch("idx:movie", "war");
            assertThat(war.count()).isEqualTo(2);
            assertThat(blockingDs.search().ftSearch("idx:movie", "brutally").count()).isEqualTo(1);
        });
    }

}
