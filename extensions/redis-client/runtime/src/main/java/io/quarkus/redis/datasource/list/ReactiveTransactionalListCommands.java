package io.quarkus.redis.datasource.list;

import java.time.Duration;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

public interface ReactiveTransactionalListCommands<K, V> extends ReactiveTransactionalRedisCommands {

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> blmove(K source, K destination, Position positionInSource, Position positionInDest, Duration timeout);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> blmpop(Duration timeout, Position position, K... keys);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> blmpop(Duration timeout, Position position, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/blpop">BLPOP</a>. Summary: Remove and get the first
     * element in a list, or block until one is available Group: list Requires Redis 2.0.0
     *
     * @param timeout
     *        the operation timeout (in seconds)
     * @param keys
     *        the keys from which the element must be popped
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> blpop(Duration timeout, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/brpop">BRPOP</a>. Summary: Remove and get the last element
     * in a list, or block until one is available Group: list Requires Redis 2.0.0
     *
     * @param timeout
     *        the operation timeout (in seconds)
     * @param keys
     *        the keys from which the element must be popped
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> brpop(Duration timeout, K... keys);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/brpoplpush
     */
    @Deprecated
    Uni<Void> brpoplpush(Duration timeout, K source, K destination);

    /**
     * Execute the command <a href="https://redis.io/commands/lindex">LINDEX</a>. Summary: Get an element from a list by
     * its index Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param index
     *        the index
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lindex(K key, long index);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> linsertBeforePivot(K key, V pivot, V element);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> linsertAfterPivot(K key, V pivot, V element);

    /**
     * Execute the command <a href="https://redis.io/commands/llen">LLEN</a>. Summary: Get the length of a list Group:
     * list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> llen(K key);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lmove(K source, K destination, Position positionInSource, Position positionInDestination);

    /**
     * Execute the command <a href="https://redis.io/commands/lmpop">LMPOP</a>. Summary: Pop one element from the first
     * non-empty list Group: list Requires Redis 7.0.0
     *
     * @param position
     *        the position of the item to pop (LEFT: beginning ot the list, RIGHT: end of the list)
     * @param keys
     *        the keys from which the item will be popped, must not be empty
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lmpop(Position position, K... keys);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lmpop(Position position, int count, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/lpop">LPOP</a>. Summary: Remove and get the first elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/lpop">LPOP</a>. Summary: Remove and get the first elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of element to pop
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/lpos">LPOS</a>. Summary: Return the index of matching
     * elements on a list Group: list Requires Redis 6.0.6
     *
     * @param key
     *        the key
     * @param element
     *        the element to find
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpos(K key, V element);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpos(K key, V element, LPosArgs args);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpos(K key, V element, int count);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpos(K key, V element, int count, LPosArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/lpush">LPUSH</a>. Summary: Prepend one or multiple
     * elements to a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param elements
     *        the elements to add
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpush(K key, V... elements);

    /**
     * Execute the command <a href="https://redis.io/commands/lpushx">LPUSHX</a>. Summary: Prepend an element to a list,
     * only if the list exists Group: list Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param elements
     *        the elements to add
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lpushx(K key, V... elements);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lrange(K key, long start, long stop);

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
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> lrem(K key, long count, V element);

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
     */
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
     */
    Uni<Void> ltrim(K key, long start, long stop);

    /**
     * Execute the command <a href="https://redis.io/commands/rpop">RPOP</a>. Summary: Remove and get the last elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> rpop(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/rpop">RPOP</a>. Summary: Remove and get the last elements
     * in a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param count
     *        the number of element to pop
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> rpop(K key, int count);

    /**
     * Execute the command <a href="https://redis.io/commands/rpoplpush">RPOPLPUSH</a>. Summary: Remove the last element
     * in a list, prepend it to another list and return it Group: list Requires Redis 1.2.0
     *
     * @param source
     *        the key
     * @param destination
     *        the key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     *
     * @deprecated See https://redis.io/commands/rpoplpush
     */
    @Deprecated
    Uni<Void> rpoplpush(K source, K destination);

    /**
     * Execute the command <a href="https://redis.io/commands/rpush">RPUSH</a>. Summary: Append one or multiple elements
     * to a list Group: list Requires Redis 1.0.0
     *
     * @param key
     *        the key
     * @param values
     *        the values to add to the list
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> rpush(K key, V... values);

    /**
     * Execute the command <a href="https://redis.io/commands/rpushx">RPUSHX</a>. Summary: Append an element to a list,
     * only if the list exists Group: list Requires Redis 2.2.0
     *
     * @param key
     *        the key
     * @param values
     *        the values to add to the list
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> rpushx(K key, V... values);
}
