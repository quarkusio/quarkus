package io.quarkus.redis.datasource.set;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

public interface TransactionalSetCommands<K, V> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/sadd">SADD</a>. Summary: Add one or more members to a set
     * Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param values
     *        the values
     */
    void sadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/scard">SCARD</a>. Summary: Get the number of members in a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void scard(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/sdiff">SDIFF</a>. Summary: Subtract multiple sets Group:
     * set Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     */
    void sdiff(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sdiffstore">SDIFFSTORE</a>. Summary: Subtract multiple
     * sets and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the key
     * @param keys
     *        the keys
     */
    void sdiffstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sinter">SINTER</a>. Summary: Intersect multiple sets
     * Group: set Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     */
    void sinter(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sintercard">SINTERCARD</a>. Summary: Intersect multiple
     * sets and return the cardinality of the result Group: set Requires Redis 7.0.0
     *
     * @param keys
     *        the keys
     */
    void sintercard(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sintercard">SINTERCARD</a>. Summary: Intersect multiple
     * sets and return the cardinality of the result Group: set Requires Redis 7.0.0
     *
     * @param limit
     *        When provided with the optional LIMIT argument (which defaults to 0 and means unlimited), if the
     *        intersection cardinality reaches limit partway through the computation, the algorithm will exit and
     *        yield limit as the cardinality.
     * @param keys
     *        the keys
     */
    void sintercard(int limit, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sinterstore">SINTERSTORE</a>. Summary: Intersect multiple
     * sets and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the key
     * @param keys
     *        the keys
     */
    void sinterstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sismember">SISMEMBER</a>. Summary: Determine if a given
     * value is a member of a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param member
     *        the member to check
     */
    void sismember(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/smembers">SMEMBERS</a>. Summary: Get all the members in a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void smembers(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/smismember">SMISMEMBER</a>. Summary: Returns the
     * membership associated with the given elements for a set Group: set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members to check
     */
    void smismember(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/smove">SMOVE</a>. Summary: Move a member from one set to
     * another Group: set Requires Redis 1.0.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     * @param member
     *        the member to move
     */
    void smove(K source, K destination, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/spop">SPOP</a>. Summary: Remove and return one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void spop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/spop">SPOP</a>. Summary: Remove and return one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void spop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a>. Summary: Get one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void srandmember(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a>. Summary: Get one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of elements to pick
     */
    void srandmember(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/srem">SREM</a>. Summary: Remove one or more members from a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     */
    void srem(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/sunion">SUNION</a>. Summary: Add multiple sets Group: set
     * Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     */
    void sunion(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sunionstore">SUNIONSTORE</a>. Summary: Add multiple sets
     * and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     */
    void sunionstore(K destination, K... keys);

}
