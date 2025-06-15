package io.quarkus.redis.datasource.hyperloglog;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing commands from the {@code hyperloglog} group. See
 * <a href="https://redis.io/commands/?group=hyperloglog">the hyperloglog command list</a> for further information about
 * these commands.
 * <p>
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the sets
 */
public interface HyperLogLogCommands<K, V> extends RedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/pfadd">PFADD</a>. Summary: Adds the specified elements to
     * the specified HyperLogLog. Group: hyperloglog Requires Redis 2.8.9
     *
     * @param key
     *        the key
     * @param values
     *        the values
     *
     * @return {@code true} at least 1 HyperLogLog internal register was altered. {@code false} otherwise.
     **/
    boolean pfadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/pfmerge">PFMERGE</a>. Summary: Merge N different
     * HyperLogLogs into a single one. Group: hyperloglog Requires Redis 2.8.9
     *
     * @param destkey
     *        the key
     * @param sourcekeys
     *        the source keys
     **/
    void pfmerge(K destkey, K... sourcekeys);

    /**
     * Execute the command <a href="https://redis.io/commands/pfcount">PFCOUNT</a>. Summary: Return the approximated
     * cardinality of the set(s) observed by the HyperLogLog at key(s). Group: hyperloglog Requires Redis 2.8.9
     *
     * @return The approximated number of unique elements observed via PFADD.
     **/
    long pfcount(K... keys);
}
