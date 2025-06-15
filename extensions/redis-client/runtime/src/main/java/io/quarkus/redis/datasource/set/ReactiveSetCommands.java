package io.quarkus.redis.datasource.set;

import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code set} group. See <a href="https://redis.io/commands/?group=set">the set
 * command list</a> for further information about these commands.
 * <p>
 * A {@code set} is a set of members of type {@code V}.
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the sets
 */
public interface ReactiveSetCommands<K, V> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/sadd">SADD</a>. Summary: Add one or more members to a set
     * Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param values
     *        the values
     *
     * @return the number of elements that were added to the set, not including all the elements already present in the
     *         set.
     **/
    Uni<Integer> sadd(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/scard">SCARD</a>. Summary: Get the number of members in a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the cardinality (number of elements) of the set, or {@code 0} key does not exist.
     **/
    Uni<Long> scard(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/sdiff">SDIFF</a>. Summary: Subtract multiple sets Group:
     * set Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return list with members of the resulting set.
     **/
    Uni<Set<V>> sdiff(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sdiffstore">SDIFFSTORE</a>. Summary: Subtract multiple
     * sets and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the key
     * @param keys
     *        the keys
     *
     * @return the number of elements in the resulting set.
     **/
    Uni<Long> sdiffstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sinter">SINTER</a>. Summary: Intersect multiple sets
     * Group: set Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return list with members of the resulting set.
     **/
    Uni<Set<V>> sinter(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sintercard">SINTERCARD</a>. Summary: Intersect multiple
     * sets and return the cardinality of the result Group: set Requires Redis 7.0.0
     *
     * @param keys
     *        the keys
     *
     * @return the number of elements in the resulting intersection.
     **/
    Uni<Long> sintercard(K... keys);

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
     * @return the number of elements in the resulting intersection.
     **/
    Uni<Long> sintercard(int limit, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sinterstore">SINTERSTORE</a>. Summary: Intersect multiple
     * sets and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the key
     * @param keys
     *        the keys
     *
     * @return the number of elements in the resulting set.
     **/
    Uni<Long> sinterstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sismember">SISMEMBER</a>. Summary: Determine if a given
     * value is a member of a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param member
     *        the member to check
     *
     * @return {@code true} the element is a member of the set. {@code false} the element is not a member of the set, or
     *         if key does not exist.
     **/
    Uni<Boolean> sismember(K key, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/smembers">SMEMBERS</a>. Summary: Get all the members in a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return all elements of the set.
     **/
    Uni<Set<V>> smembers(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/smismember">SMISMEMBER</a>. Summary: Returns the
     * membership associated with the given elements for a set Group: set Requires Redis 6.2.0
     *
     * @param key
     *        the key
     * @param members
     *        the members to check
     *
     * @return list representing the membership of the given elements, in the same order as they are requested.
     **/
    Uni<List<Boolean>> smismember(K key, V... members);

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
     * @return {@code true} the element is moved. {@code false} the element is not a member of source and no operation
     *         was performed.
     **/
    Uni<Boolean> smove(K source, K destination, V member);

    /**
     * Execute the command <a href="https://redis.io/commands/spop">SPOP</a>. Summary: Remove and return one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the removed member, or {@code null} when key does not exist.
     **/
    Uni<V> spop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/spop">SPOP</a>. Summary: Remove and return one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the removed members, or an empty array when key does not exist.
     **/
    Uni<Set<V>> spop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a>. Summary: Get one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the randomly selected element, or {@code null} when key does not exist.
     **/
    Uni<V> srandmember(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a>. Summary: Get one or multiple
     * random members from a set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of elements to pick
     *
     * @return an array of elements, or an empty array when key does not exist.
     **/
    Uni<List<V>> srandmember(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/srem">SREM</a>. Summary: Remove one or more members from a
     * set Group: set Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the number of members that were removed from the set, not including non-existing members.
     **/
    Uni<Integer> srem(K key, V... members);

    /**
     * Execute the command <a href="https://redis.io/commands/sunion">SUNION</a>. Summary: Add multiple sets Group: set
     * Requires Redis 1.0.0
     *
     * @param keys
     *        the keys
     *
     * @return list with members of the resulting set.
     **/
    Uni<Set<V>> sunion(K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sunionstore">SUNIONSTORE</a>. Summary: Add multiple sets
     * and store the resulting set in a key Group: set Requires Redis 1.0.0
     *
     * @param destination
     *        the destination key
     * @param keys
     *        the keys
     *
     * @return the number of elements in the resulting set.
     **/
    Uni<Long> sunionstore(K destination, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/sscan">SSCAN</a>. Summary: Incrementally iterate Set
     * elements Group: set Requires Redis 2.8.0
     *
     * @param key
     *        the key
     *
     * @return the cursor
     **/
    ReactiveSScanCursor<V> sscan(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/sscan">SSCAN</a>. Summary: Incrementally iterate Set
     * elements Group: set Requires Redis 2.8.0
     *
     * @param key
     *        the key
     * @param scanArgs
     *        the extra parameters
     *
     * @return the cursor
     **/
    ReactiveSScanCursor<V> sscan(K key, ScanArgs scanArgs);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a>. Summary: Sort the elements in a list, set
     * or sorted set Group: generic Requires Redis 1.0.0
     *
     * @return the list of sorted elements.
     **/
    Uni<List<V>> sort(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a>. Summary: Sort the elements in a list, set
     * or sorted set Group: generic Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param sortArguments
     *        the {@code SORT} command extra-arguments
     *
     * @return the list of sorted elements.
     **/
    Uni<List<V>> sort(K key, SortArgs sortArguments);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a> with the {@code STORE} option. Summary:
     * Sort the elements in a list, set or sorted set Group: generic Requires Redis 1.0.0
     *
     * @param sortArguments
     *        the SORT command extra-arguments
     *
     * @return the number of sorted elements in the destination list.
     **/
    Uni<Long> sortAndStore(K key, K destination, SortArgs sortArguments);

    /**
     * Execute the command <a href="https://redis.io/commands/sort">SORT</a> with the {@code STORE} option. Summary:
     * Sort the elements in a list, set or sorted set Group: generic Requires Redis 1.0.0
     *
     * @return the number of sorted elements in the destination list.
     **/
    Uni<Long> sortAndStore(K key, K destination);
}
