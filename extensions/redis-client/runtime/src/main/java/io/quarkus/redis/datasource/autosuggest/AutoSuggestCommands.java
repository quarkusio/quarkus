package io.quarkus.redis.datasource.autosuggest;

import java.util.List;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing commands from the {@code auto-suggest} group (requires the Redis Search module from Redis stack).
 * See <a href="https://redis.io/commands/?group=suggestion">the auto-suggest command list</a> for further information
 * about these commands.
 *
 * @param <K>
 *        the type of the key
 */
public interface AutoSuggestCommands<K> extends RedisCommands {

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
     * @return A uni emitting the current size of the suggestion dictionary.
     */
    default long ftSugAdd(K key, String string, double score) {
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
     * @return A uni emitting the current size of the suggestion dictionary.
     */
    long ftSugAdd(K key, String string, double score, boolean increment);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugdel/">FT.SUGDEL</a>. Summary: Delete a string from a
     * suggestion index Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param string
     *        the suggestion string to index
     *
     * @return A uni emitting {@code true} if the value was found, {@code false} otherwise
     */
    boolean ftSugDel(K key, String string);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.sugget/">FT.SUGGET</a>. Summary: Get completion
     * suggestions for a prefix Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     * @param prefix
     *        is prefix to complete on.
     *
     * @return A uni emitting a list of the top suggestions matching the prefix, optionally with score after each entry.
     */
    List<Suggestion> ftSugGet(K key, String prefix);

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
     * @return A uni emitting {@code true} if the value was found, {@code false} otherwise
     */
    List<Suggestion> ftSugGet(K key, String prefix, GetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/ft.suglen/">FT.SUGLEN</a>. Summary: Get the size of an
     * auto-complete suggestion dictionary Group: auto-suggest
     *
     * @param key
     *        the suggestion dictionary key
     *
     * @return A uni emitting the current size of the suggestion dictionary.
     */
    long ftSugLen(K key);
}
