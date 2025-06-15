package io.quarkus.redis.datasource.autosuggest;

import io.quarkus.redis.datasource.TransactionalRedisCommands;

/**
 * Allows executing commands from the {@code auto-suggest} group (requires the Redis Search module from Redis stack).
 * See <a href="https://redis.io/commands/?group=suggestion">the auto-suggest command list</a> for further information
 * about these commands. This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command
 * methods return {@code void}.
 *
 * @param <K>
 *        the type of the key
 */
public interface TransactionalAutoSuggestCommands<K> extends TransactionalRedisCommands {

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
     */
    default void ftSugAdd(K key, String string, double score) {
        ftSugAdd(key, string, score, false);
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
     */
    void ftSugAdd(K key, String string, double score, boolean increment);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugdel/">FT.SUGDEL</a>. Summary: Delete a string from a
     * suggestion index Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param string
     *        the suggestion string to index
     */
    void ftSugDel(K key, String string);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugget/">FT.SUGGET</a>. Summary: Get completion
     * suggestions for a prefix Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param prefix
     *        is prefix to complete on.
     */
    void ftSugget(K key, String prefix);

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
     */
    void ftSugget(K key, String prefix, GetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.suglen/">FT.SUGLEN</a>. Summary: Get the size of an
     * auto-complete suggestion dictionary Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     */
    void ftSugLen(K key);
}
