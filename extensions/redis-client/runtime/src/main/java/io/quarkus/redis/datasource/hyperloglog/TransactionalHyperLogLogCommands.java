package io.quarkus.redis.datasource.hyperloglog;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

public interface TransactionalHyperLogLogCommands<K, V> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/pfadd">PFADD</a>. Summary: Adds the specified elements to
     * the specified HyperLogLog. Group: hyperloglog Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param values
     *        the values
     */
    void pfadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/pfmerge">PFMERGE</a>. Summary: Merge N different
     * HyperLogLogs into a single one. Group: hyperloglog Requires Redis 2.8.9
     *
     * @param destkey
     *        the key
     * @param sourcekeys
     *        the source keys
     */
    void pfmerge(K destkey, K... sourcekeys);

    /**
     * Execute the command <a href="https://redis.io/commands/pfcount">PFCOUNT</a>. Summary: Return the approximated
     * cardinality of the set(s) observed by the HyperLogLog at key(s). Group: hyperloglog Requires Redis 2.8.9
     */
    void pfcount(K... keys);
}
