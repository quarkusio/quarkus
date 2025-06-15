package io.quarkus.redis.datasource.graph;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code graph} group. These commands require the
 * <a href="https://redis.io/docs/stack/graph/">Redis Graph</a> module to be installed in the Redis server.
 * <p>
 * See <a href="https://redis.io/commands/?group=graph">the graph command list</a> for further information about these
 * commands.
 * <p>
 *
 * @param <K>
 *        the type of the key
 */
public interface ReactiveGraphCommands<K> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/graph.delete">GRAPH.DELETE</a>. Summary: Completely
     * removes the graph and all of its entities. Group: graph
     *
     * @param key
     *        the key, must not be {@code null}
     *
     * @return a uni producing {@code null} when the operation completes
     **/
    Uni<Void> graphDelete(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/graph.delete">GRAPH.EXPLAIN</a>. Summary: Constructs a
     * query execution plan but does not run it. Inspect this execution plan to better understand how your query will
     * get executed. Group: graph
     * <p>
     *
     * @param key
     *        the key, must not be {@code null}
     * @param query
     *        the query, must not be {@code null}
     *
     * @return a uni producing the string representation of the query execution plan
     */
    Uni<String> graphExplain(K key, String query);

    /**
     * Execute the command <a href="https://redis.io/commands/graph.list">GRAPH.LIST</a>. Summary: Lists all graph keys
     * in the keyspace. Group: graph
     * <p>
     *
     * @return a uni producing the list of list of keys storing graphs
     */
    Uni<List<K>> graphList();

    /**
     * Execute the command <a href="https://redis.io/commands/graph.delete">GRAPH.QUERY</a>. Summary: Executes the given
     * query against a specified graph. Group: graph
     * <p>
     *
     * @param key
     *        the key, must not be {@code null}
     * @param query
     *        the query, must not be {@code null}
     *
     * @return a map, potentially empty, containing the requested items to return. The map is empty if the query does
     *         not have a {@code return} clause. For each request item, a {@link GraphQueryResponseItem} is returned. It
     *         can represent a scalar item
     *         ({@link io.quarkus.redis.datasource.graph.GraphQueryResponseItem.ScalarItem}), a node
     *         ({@link io.quarkus.redis.datasource.graph.GraphQueryResponseItem.NodeItem}, or a relation
     *         ({@link io.quarkus.redis.datasource.graph.GraphQueryResponseItem.RelationItem}).
     */
    Uni<List<Map<String, GraphQueryResponseItem>>> graphQuery(K key, String query);

    /**
     * Execute the command <a href="https://redis.io/commands/graph.delete">GRAPH.QUERY</a>. Summary: Executes the given
     * query against a specified graph. Group: graph
     * <p>
     *
     * @param key
     *        the key, must not be {@code null}
     * @param query
     *        the query, must not be {@code null}
     * @param timeout
     *        a timeout, must not be {@code null}
     *
     * @return a map, potentially empty, containing the requested items to return. The map is empty if the query does
     *         not have a {@code return} clause. For each request item, a {@link GraphQueryResponseItem} is returned. It
     *         can represent a scalar item
     *         ({@link io.quarkus.redis.datasource.graph.GraphQueryResponseItem.ScalarItem}), a node
     *         ({@link io.quarkus.redis.datasource.graph.GraphQueryResponseItem.NodeItem}, or a relation
     *         ({@link io.quarkus.redis.datasource.graph.GraphQueryResponseItem.RelationItem}).
     */
    Uni<List<Map<String, GraphQueryResponseItem>>> graphQuery(K key, String query, Duration timeout);

}
