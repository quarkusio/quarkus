package io.quarkus.redis.datasource.autosuggest;

import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.smallrye.mutiny.Uni;

/**
 * Allows executing commands from the {@code auto-suggest} group (requires the Redis Search module from Redis stack).
 * See <a href="https://redis.io/commands/?group=suggestion">the auto-suggest command list</a> for further information
 * about these commands. This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command
 * methods return {@code Uni<Void>}.
 *
 * @param <K>
 *        the type of the key
 */
public interface ReactiveTransactionalAutoSuggestCommands<K> extends ReactiveTransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugadd/">FT.SUGADD</a>. Summary: Add a suggestion
     * string to an auto-complete suggestion dictionary Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param string
     *        the suggestion string to index
     * @param score
     *        the floating point number of the suggestion string's weight
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    default Uni<Void> ftSugAdd(K key, String string, double score) {
        return ftSugAdd(key, string, score, false);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugadd/">FT.SUGADD</a>. Summary: Add a suggestion
     * string to an auto-complete suggestion dictionary Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param string
     *        the suggestion string to index
     * @param score
     *        the floating point number of the suggestion string's weight
     * @param increment
     *        increments the existing entry of the suggestion by the given score, instead of replacing the score.
     *        This is useful for updating the dictionary based on user queries in real time.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> ftSugAdd(K key, String string, double score, boolean increment);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugdel/">FT.SUGDEL</a>. Summary: Delete a string from a
     * suggestion index Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param string
     *        the suggestion string to index
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> ftSugDel(K key, String string);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugget/">FT.SUGGET</a>. Summary: Get completion
     * suggestions for a prefix Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param prefix
     *        is prefix to complete on.
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> ftSugget(K key, String prefix);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugget/">FT.SUGGET</a>. Summary: Get completion
     * suggestions for a prefix Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param prefix
     *        is prefix to complete on.
     * @param args
     *        the extra argument, must not be {@code null}
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> ftSugget(K key, String prefix, GetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.suglen/">FT.SUGLEN</a>. Summary: Get the size of an
     * auto-complete suggestion dictionary Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     *
     * @return A {@code Uni} emitting {@code null} when the command has been enqueued successfully in the transaction, a
     *         failure otherwise. In the case of failure, the transaction is discarded.
     */
    Uni<Void> ftSugLen(K key);
}
