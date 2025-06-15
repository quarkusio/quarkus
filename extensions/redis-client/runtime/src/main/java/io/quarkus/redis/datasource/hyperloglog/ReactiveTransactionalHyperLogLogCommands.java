package io.quarkus.redis.datasource.hyperloglog;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalHyperLogLogCommands<K, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/pfadd">PFADD</a>. Summary: Adds the specified elements to
     * the specified HyperLogLog. Group: hyperloglog Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param values
     *        the values
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pfadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/pfmerge">PFMERGE</a>. Summary: Merge N different
     * HyperLogLogs into a single one. Group: hyperloglog Requires Redis 2.8.9
     *
     * @param destkey
     *        the key
     * @param sourcekeys
     *        the source keys
     */
    Uni<Void> pfmerge(K destkey, K... sourcekeys);

    /**
     * Execute the command <a href="https://redis.io/commands/pfcount">PFCOUNT</a>. Summary: Return the approximated
     * cardinality of the set(s) observed by the HyperLogLog at key(s). Group: hyperloglog Requires Redis 2.8.9
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> pfcount(K... keys);
}
