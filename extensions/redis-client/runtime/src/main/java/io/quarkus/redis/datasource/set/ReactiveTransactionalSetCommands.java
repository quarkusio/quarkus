package io.quarkus.redis.datasource.set;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalSetCommands<K, V> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/sadd">SADD</a>. Summary: Add one or more members to a set
     * Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param values
     *        the values
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/scard">SCARD</a>. Summary: Get the number of members in a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> scard(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/sdiff">SDIFF</a>. Summary: Subtract multiple sets Group:
     * set Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sdiff(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sdiffstore">SDIFFSTORE</a>. Summary: Subtract multiple
     * sets and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sdiffstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sinter">SINTER</a>. Summary: Intersect multiple sets
     * Group: set Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sinter(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sintercard">SINTERCARD</a>. Summary: Intersect multiple
     * sets and return the cardinality of the result Group: set Requires Redis 7.0.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sintercard(K... keys);

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
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sintercard(int limit, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sinterstore">SINTERSTORE</a>. Summary: Intersect multiple
     * sets and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sinterstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sismember">SISMEMBER</a>. Summary: Determine if a given
     * value is a member of a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param member
     *        the member to check
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sismember(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/smembers">SMEMBERS</a>. Summary: Get all the members in a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> smembers(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/smismember">SMISMEMBER</a>. Summary: Returns the
     * membership associated with the given elements for a set Group: set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members to check
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> smismember(K key, V... members);

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
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> smove(K source, K destination, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/spop">SPOP</a>. Summary: Remove and return one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> spop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/spop">SPOP</a>. Summary: Remove and return one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> spop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a>. Summary: Get one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> srandmember(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a>. Summary: Get one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of elements to pick
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> srandmember(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/srem">SREM</a>. Summary: Remove one or more members from a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> srem(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/sunion">SUNION</a>. Summary: Add multiple sets Group: set
     * Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sunion(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sunionstore">SUNIONSTORE</a>. Summary: Add multiple sets
     * and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> sunionstore(K destination, K... keys);

}
