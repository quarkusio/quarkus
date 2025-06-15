package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.search.CreateArgs;
import io.quarkus.redis.datasource.search.FieldOptions;
import io.quarkus.redis.datasource.search.FieldType;
import io.quarkus.redis.datasource.search.IndexedField;
import io.quarkus.redis.datasource.search.QueryArgs;
import io.quarkus.redis.datasource.search.ReactiveTransactionalSearchCommands;
import io.quarkus.redis.datasource.search.TransactionalSearchCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@RequiresCommand("ft.create")
public class TransactionalSearchCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    void setup() {
        var hash = blocking.hash(String.class);
        hash.hset("movie:11002", Map.of("title", "Star Wars: Episode V - The Empire Strikes Back", "plot",
                "After the Rebels are brutally overpowered by the Empire on the ice planet Hoth, ...", "release_year",
                "1972", "genre", "Action", "rating", "8.7", "votes", "1127635", "imbd_id", "tt0080684"));
        hash.hset("movie:11003", Map.of("title", "The Godfather", "plot",
                "The aging patriarch of an organized crime dynasty transfers control of his ...", "release_year",
                "1972", "genre", "Drama", "rating", "9.2", "votes", "1563839", "imbd_id", "tt0068646"));
        hash.hset("movie:11004",
                Map.of("title", "Heat", "plot", "A group of professional bank robbers start to feel the heat ...",
                        "release_year", "1995", "genre", "Thriller", "rating", "8.2", "votes", "559490", "imbd_id",
                        "tt0113277"));
        hash.hset("movie:11005",
                Map.of("title", "Star Wars: Episode VI - Return of the Jedi", "plot",
                        "The Rebels dispatch to Endor to destroy the second Empire's Death Star.", "release_year",
                        "1983", "genre", "Action", "rating", "8.3", "votes", "906260", "imbd_id", "tt0086190"));
    }

    @Test
    public void transactionalSearchBlocking() {
        setup();

        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalSearchCommands search = tx.search();
            assertThat(search.getDataSource()).isEqualTo(tx);
            search.ftCreate("idx:movie",
                    new CreateArgs().onHash().prefixes("movie:")
                            .indexedField("title", FieldType.TEXT, new FieldOptions().sortable())
                            .indexedField("release_year", FieldType.NUMERIC, new FieldOptions().sortable())
                            .indexedField("rating", FieldType.NUMERIC, new FieldOptions().sortable())
                            .indexedField("genre", FieldType.TAG, new FieldOptions().sortable()));
            search.ftSearch("idx:movie", "war");
            search.ftAlter("idx:movie", IndexedField.from("plot", FieldType.TEXT, new FieldOptions().weight(0.5)));
            search.ftSearch("idx:movie", "empire @genre:{Action}", new QueryArgs().returnAttribute("title"));
        });

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.discarded()).isFalse();
    }

    @Test
    public void transactionalSearchReactive() {
        setup();

        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalSearchCommands search = tx.search();
            assertThat(search.getDataSource()).isEqualTo(tx);
            var u1 = search.ftCreate("idx:movie",
                    new CreateArgs().onHash().prefixes("movie:")
                            .indexedField("title", FieldType.TEXT, new FieldOptions().sortable())
                            .indexedField("release_year", FieldType.NUMERIC, new FieldOptions().sortable())
                            .indexedField("rating", FieldType.NUMERIC, new FieldOptions().sortable())
                            .indexedField("genre", FieldType.TAG, new FieldOptions().sortable()));
            var u2 = search.ftSearch("idx:movie", "war");
            var u3 = search.ftAlter("idx:movie",
                    IndexedField.from("plot", FieldType.TEXT, new FieldOptions().weight(0.5)));
            var u4 = search.ftSearch("idx:movie", "empire @genre:{Action}", new QueryArgs().returnAttribute("title"));

            return u1.chain(() -> u2).chain(() -> u3).chain(() -> u4);
        }).await().indefinitely();

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.discarded()).isFalse();
    }

}
