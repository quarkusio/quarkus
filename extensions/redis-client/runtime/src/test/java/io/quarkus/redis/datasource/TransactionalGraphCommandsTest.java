package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.graph.GraphQueryResponseItem;
import io.quarkus.redis.datasource.graph.ReactiveTransactionalGraphCommands;
import io.quarkus.redis.datasource.graph.TransactionalGraphCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;

@SuppressWarnings("unchecked")
@RequiresCommand("graph.query")
public class TransactionalGraphCommandsTest extends DatasourceTestBase {

    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    static final String createQuery = "CREATE (:Rider {name:'Valentino Rossi'})-[:rides]->(:Team {name:'Yamaha'}), (:Rider {name:'Dani Pedrosa'})-[:rides]->(:Team {name:'Honda'}), (:Rider {name:'Andrea Dovizioso'})-[:rides]->(:Team {name:'Ducati'})";
    static final String query = "MATCH (r:Rider)-[:rides]->(t:Team) WHERE t.name = 'Yamaha' RETURN r.name, t.name";

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(60));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    @Test
    public void graphBlocking() {
        TransactionResult result = blocking.withTransaction(tx -> {
            TransactionalGraphCommands<String> graph = tx.graph();
            assertThat(graph.getDataSource()).isEqualTo(tx);
            graph.graphQuery("moto", createQuery); // void
            graph.graphQuery("moto", query); // result
            graph.graphList(); // "moto"
            graph.graphDelete("moto"); // void
            graph.graphList(); // empty
        });

        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((List<Map<String, GraphQueryResponseItem>>) result.get(0)).isEmpty();
        assertThat((List<Map<String, GraphQueryResponseItem>>) result.get(1)).hasSize(1).allSatisfy(map -> {
            GraphQueryResponseItem.ScalarItem driver = map.get("r.name").asScalarItem();
            GraphQueryResponseItem.ScalarItem team = map.get("t.name").asScalarItem();
            assertThat(driver.asString()).isEqualTo("Valentino Rossi");
            assertThat(driver.name()).isEqualTo("r.name");
            assertThat(team.asString()).isEqualTo("Yamaha");
            assertThat(team.name()).isEqualTo("t.name");
        });
        assertThat((List<String>) result.get(2)).hasSize(1).containsExactly("moto");
        assertThat((Object) result.get(3)).isNull();
        assertThat((List<String>) result.get(4)).hasSize(0);
    }

    @Test
    public void graphReactive() {
        TransactionResult result = reactive.withTransaction(tx -> {
            ReactiveTransactionalGraphCommands<String> graph = tx.graph();
            assertThat(graph.getDataSource()).isEqualTo(tx);
            return graph.graphQuery("moto", createQuery) // void
                    .chain(() -> graph.graphQuery("moto", query)) // result
                    .chain(graph::graphList) // "moto"
                    .chain(() -> graph.graphDelete("moto")) // void
                    .chain(graph::graphList); // empty
        }).await().indefinitely();
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.discarded()).isFalse();
        assertThat((List<Map<String, GraphQueryResponseItem>>) result.get(0)).isEmpty();
        assertThat((List<Map<String, GraphQueryResponseItem>>) result.get(1)).hasSize(1).allSatisfy(map -> {
            GraphQueryResponseItem.ScalarItem driver = map.get("r.name").asScalarItem();
            GraphQueryResponseItem.ScalarItem team = map.get("t.name").asScalarItem();
            assertThat(driver.asString()).isEqualTo("Valentino Rossi");
            assertThat(driver.name()).isEqualTo("r.name");
            assertThat(team.asString()).isEqualTo("Yamaha");
            assertThat(team.name()).isEqualTo("t.name");
        });
        assertThat((List<String>) result.get(2)).hasSize(1).containsExactly("moto");
        assertThat((Object) result.get(3)).isNull();
        assertThat((List<String>) result.get(4)).hasSize(0);
    }

}
