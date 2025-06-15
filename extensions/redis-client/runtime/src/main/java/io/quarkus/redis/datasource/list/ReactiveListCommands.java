package io.quarkus.redis.datasource.list;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.SortArgs;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code list} group. See <a href="https://redis.io/commands/?group=list">the list
 * command list</a> for further information about these commands.
 * <p>
 * A {@code list} is a bag of members of type {@code V}. Unlike {@code set}, you can have duplicated, and unlike
 * {@code sorted set}, the members are not sorted.
 *
 * @param <K>
 *        the type of the key
 * @param <V>
 *        the type of the value stored in the lists
 */
public interface ReactiveListCommands<K, V> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/blmove">BLMOVE</a>. Summary: Pop an element from a list,
     * push it to another list and return it; or block until one is available Group: list Requires Redis 6.2.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     * @param positionInSource
     *        the position of the element in the source, {@code LEFT} means the first element, {@code RIGHT} means
     *        the last element.
     * @param positionInDest
     *        the position of the element in the destination, {@code LEFT} means the first element, {@code RIGHT}
     *        means the last element.
     * @param timeout
     *        the operation timeout (in seconds)
     *
     * @return the element being popped from source and pushed to destination. If timeout is reached, a Null reply is
     *         returned.
     **/
    Uni<V> blmove(K source, K destination, Position positionInSource, Position positionInDest, Duration timeout);

    /**
     * Execute the command <a href="https://redis.io/commands/blmpop">BLMPOP</a>. Summary: Pop elements from a list, or
     * block until one is available Group: list Requires Redis 7.0.0
     *
     * @param timeout
     *        the operation timeout (in seconds)
     * @param position
     *        whether if the element must be popped from the beginning of the list ({@code LEFT}) or from the end
     *        ({@code RIGHT})
     * @param keys
     *        the keys from which the element must be popped
     *
     * @return {@code null} when no element could be popped, and timeout is reached, otherwise the key/value structure
     **/
    Uni<KeyValue<K, V>> blmpop(Duration timeout, Position position, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/blmpop">BLMPOP</a>. Summary: Pop elements from a list, or
     * block until one is available Group: list Requires Redis 7.0.0
     *
     * @param timeout
     *        the operation timeout (in seconds)
     * @param position
     *        whether if the element must be popped from the beginning of the list ({@code LEFT}) or from the end
     *        ({@code RIGHT})
     * @param count
     *        the number of element to pop
     * @param keys
     *        the keys from which the element must be popped
     *
     * @return {@code null} when no element could be popped, and timeout is reached, otherwise the list of key/value
     *         structures
     **/
    Uni<List<KeyValue<K, V>>> blmpop(Duration timeout, Position position, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/blpop">BLPOP</a>. Summary: Remove and get the first
     * element in a list, or block until one is available Group: list Requires Redis 2.0.0
     *
     * @param timeout
     *        the operation timeout (in seconds)
     * @param keys
     *        the keys from which the element must be popped
     *
     * @return A {@code null} multi-bulk when no element could be popped and the timeout expired, otherwise the
     *         key/value structure.
     **/
    Uni<KeyValue<K, V>> blpop(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/brpop">BRPOP</a>. Summary: Remove and get the last element
     * in a list, or block until one is available Group: list Requires Redis 2.0.0
     *
     * @param timeout
     *        the operation timeout (in seconds)
     * @param keys
     *        the keys from which the element must be popped
     *
     * @return A {@code null} multi-bulk when no element could be popped and the timeout expired, otherwise the
     *         key/value structure.
     **/
    Uni<KeyValue<K, V>> brpop(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/brpoplpush">BRPOPLPUSH</a>. Summary: Pop an element from a
     * list, push it to another list and return it; or block until one is available Group: list Requires Redis 2.2.0
     *
     * @param timeout
     *        the timeout, in seconds
     * @param source
     *        the source key
     * @param destination
     *        the detination key
     *
     * @return the element being popped from source and pushed to destination. If timeout is reached, a Null reply is
     *         returned.
     *
     * @deprecated See https://redis.io/commands/brpoplpush
     **/
    @Deprecated
    Uni<V> brpoplpush(Duration timeout, K source, K destination);

    /**
     * Execute the command <a href="https://redis.io/commands/lindex">LINDEX</a>. Summary: Get an element from a list by
     * its index Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param index
     *        the index
     *
     * @return the requested element, or {@code null} when index is out of range.
     **/
    Uni<V> lindex(K key, long index);

    /**
     * Execute the command <a href="https://redis.io/commands/linsert">LINSERT</a>. Summary: Insert an element before
     * another element in a list Group: list Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param pivot
     *        the pivot, i.e. the position reference
     * @param element
     *        the element to insert
     *
     * @return the length of the list after the insert operation, or -1 when the value {@code pivot} was not found.
     **/
    Uni<Long> linsertBeforePivot(K key, V pivot, V element);

    /**
     * Execute the command <a href="https://redis.io/commands/linsert">LINSERT</a>. Summary: Insert an element after
     * another element in a list Group: list Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param pivot
     *        the pivot, i.e. the position reference
     * @param element
     *        the element to insert
     *
     * @return the length of the list after the insert operation, or -1 when the value {@code pivot} was not found.
     **/
    Uni<Long> linsertAfterPivot(K key, V pivot, V element);

    /**
     * Execute the command <a href="https://redis.io/commands/llen">LLEN</a>. Summary: Get the length of a list Group:
     * list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the length of the list at key, if the list is empty, 0 is returned.
     **/
    Uni<Long> llen(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/lmove">LMOVE</a>. Summary: Pop an element from a list,
     * push it to another list and return it Group: list Requires Redis 6.2.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     * @param positionInSource
     *        the position of the element to pop in the source (LEFT: first element, RIGHT: last element)
     * @param positionInDestination
     *        the position of the element to insert in the destination (LEFT: first element, RIGHT: last element)
     *
     * @return the element being popped and pushed.
     **/
    Uni<V> lmove(K source, K destination, Position positionInSource, Position positionInDestination);

    /**
     * Execute the command <a href="https://redis.io/commands/lmpop">LMPOP</a>. Summary: Pop one element from the first
     * non-empty list Group: list Requires Redis 7.0.0
     *
     * @param position
     *        the position of the item to pop (LEFT: beginning ot the list, RIGHT: end of the list)
     * @param keys
     *        the keys from which the item will be popped, must not be empty
     *
     * @return A {@code null} when no element could be popped. A {@link KeyValue} with the key and popped value.
     **/
    Uni<KeyValue<K, V>> lmpop(Position position, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/lmpop">LMPOP</a>. Summary: Pop {@code count} elements from
     * the first non-empty list Group: list Requires Redis 7.0.0
     *
     * @param position
     *        the position of the item to pop (LEFT: beginning ot the list, RIGHT: end of the list)
     * @param count
     *        the number of items to pop
     * @param keys
     *        the keys from which the item will be popped, must not be empty
     *
     * @return A {@code empty} when no element could be popped. A list of {@link KeyValue} with at most count items.
     **/
    Uni<List<KeyValue<K, V>>> lmpop(Position position, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/lpop">LPOP</a>. Summary: Remove and get the first elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the value of the first element, or {@code null} when key does not exist.
     **/
    Uni<V> lpop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/lpop">LPOP</a>. Summary: Remove and get the first elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of element to pop
     *
     * @return the popped elements (at most {@code count}), or {@code empty} when key does not exist.
     **/
    Uni<List<V>> lpop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/lpos">LPOS</a>. Summary: Return the index of matching
     * elements on a list Group: list Requires Redis 6.0.6
     *
     * @param key
     *        the key
     * @param element
     *        the element to find
     *
     * @return The command returns the integer representing the matching element, or {@code null} if there is no match.
     **/
    Uni<Long> lpos(K key, V element);

    /**
     * Execute the command <a href="https://redis.io/commands/lpos">LPOS</a>. Summary: Return the index of matching
     * elements on a list Group: list Requires Redis 6.0.6
     *
     * @param key
     *        the key
     * @param element
     *        the element to find
     * @param args
     *        the extra command parameter
     *
     * @return The command returns the integer representing the matching element, or {@code null} if there is no match.
     **/
    Uni<Long> lpos(K key, V element, LPosArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/lpos">LPOS</a>. Summary: Return the index of matching
     * elements on a list Group: list Requires Redis 6.0.6
     *
     * @param key
     *        the key
     * @param element
     *        the element to find
     * @param count
     *        the number of occurrence to find
     *
     * @return the list of positions (empty if there are no matches).
     **/
    Uni<List<Long>> lpos(K key, V element, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/lpos">LPOS</a>. Summary: Return the index of matching
     * elements on a list Group: list Requires Redis 6.0.6
     *
     * @param key
     *        the key
     * @param element
     *        the element to find
     * @param count
     *        the number of occurrence to find
     *
     * @return the list of positions (empty if there are no matches).
     **/
    Uni<List<Long>> lpos(K key, V element, int count, LPosArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/lpush">LPUSH</a>. Summary: Prepend one or multiple
     * elements to a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param elements
     *        the elements to add
     *
     * @return the length of the list after the push operations.
     **/
    Uni<Long> lpush(K key, V... elements);

    /**
     * Execute the command <a href="https://redis.io/commands/lpushx">LPUSHX</a>. Summary: Prepend an element to a list,
     * only if the list exists Group: list Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param elements
     *        the elements to add
     *
     * @return the length of the list after the push operation.
     **/
    Uni<Long> lpushx(K key, V... elements);

    /**
     * Execute the command <a href="https://redis.io/commands/lrange">LRANGE</a>. Summary: Get a range of elements from
     * a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param start
     *        the starting position
     * @param stop
     *        the last position
     *
     * @return list of elements in the specified range.
     **/
    Uni<List<V>> lrange(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/lrem">LREM</a>. Summary: Remove elements from a list
     * Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of occurence to remove, following the given rules: if count > 0: Remove elements equal to
     *        element moving from head to tail. if count < 0: Remove elements equal to element moving from tail to
     *        head. if count = 0: Remove all elements equal to element.
     * @param element
     *        the element to remove
     *
     * @return the number of removed elements.
     **/
    Uni<Long> lrem(K key, long count, V element);

    /**
     * Execute the command <a href="https://redis.io/commands/lset">LSET</a>. Summary: Set the value of an element in a
     * list by its index Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param index
     *        the index
     * @param element
     *        the element to insert
     *
     * @return a Uni producing {@code null} on success, a failure otherwise
     **/
    Uni<Void> lset(K key, long index, V element);

    /**
     * Execute the command <a href="https://redis.io/commands/ltrim">LTRIM</a>. Summary: Trim a list to the specified
     * range Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param start
     *        the starting index
     * @param stop
     *        the last index
     *
     * @return a Uni producing {@code null} on success, a failure otherwise
     **/
    Uni<Void> ltrim(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/rpop">RPOP</a>. Summary: Remove and get the last elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return the value of the last element, or {@code null} when key does not exist.
     **/
    Uni<V> rpop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/rpop">RPOP</a>. Summary: Remove and get the last elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of element to pop
     *
     * @return the list of popped elements, or {@code null} when key does not exist.
     **/
    Uni<List<V>> rpop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/rpoplpush">RPOPLPUSH</a>. Summary: Remove the last element
     * in a list, prepend it to another list and return it Group: list Requires Redis 1.2.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     *
     * @return the element being popped and pushed, or {@code null} if the source does not exist
     *
     * @deprecated See https://redis.io/commands/rpoplpush
     **/
    @Deprecated
    Uni<V> rpoplpush(K source, K destination);

    /**
     * Execute the command <a href="https://redis.io/commands/rpush">RPUSH</a>. Summary: Append one or multiple elements
     * to a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param values
     *        the values to add to the list
     *
     * @return the length of the list after the push operation.
     **/
    Uni<Long> rpush(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/rpushx">RPUSHX</a>. Summary: Append an element to a list,
     * only if the list exists Group: list Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param values
     *        the values to add to the list
     *
     * @return the length of the list after the push operation.
     **/
    Uni<Long> rpushx(K key, V... values);

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
