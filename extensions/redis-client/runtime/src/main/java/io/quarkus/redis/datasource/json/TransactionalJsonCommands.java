package io.quarkus.redis.datasource.json;

import io.quarkus.redis.datasource.TransactionalRedisCommands;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TransactionalJsonCommands<K> extends TransactionalRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param value the value, encoded to JSON
     * @param <T> the type for the value
     */
    <T> void jsonSet(K key, String path, T value);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param value the value, encoded to JSON
     * @param <T> the type for the value
     **/
    default <T> void jsonSet(K key, T value) {
        jsonSet(key, "$", value);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON object to store, must not be {@code null}
     */
    void jsonSet(K key, String path, JsonObject json);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     * <p>
     * This variant uses {@code $} as path.
     *
     * @param key the key, must not be {@code null}
     * @param json the JSON object to store, must not be {@code null}
     **/
    default void jsonSet(K key, JsonObject json) {
        jsonSet(key, "$", json);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param json the JSON array to store, must not be {@code null}
     **/
    default void jsonSet(K key, JsonArray json) {
        jsonSet(key, "$", json);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON object to store, must not be {@code null}
     * @param args the extra arguments
     */
    void jsonSet(K key, String path, JsonObject json, JsonSetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON array to store, must not be {@code null}
     */
    void jsonSet(K key, String path, JsonArray json);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON array to store, must not be {@code null}
     * @param args the extra arguments
     */
    void jsonSet(K key, String path, JsonArray json, JsonSetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param value the value to store, encoded to JSON.
     * @param args the extra arguments
     */
    <T> void jsonSet(K key, String path, T value, JsonSetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     * This method uses the root path ({@code $}).
     * It maps the retrieve JSON document to an object of type {@code <T>}.
     *
     * @param key the key, must not be {@code null}
     * @param clazz the type of object to recreate from the JSON content
     */
    <T> void jsonGet(K key, Class<T> clazz);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     * This method uses the root path ({@code $}).
     * Unlike {@link #jsonGet(Object, Class)}, it returns a {@link JsonObject}.
     *
     * @param key the key, must not be {@code null}
     */
    void jsonGetObject(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     * This method uses the root path ({@code $}).
     * Unlike {@link #jsonGet(Object, Class)}, it returns a {@link JsonArray}.
     *
     * @param key the key, must not be {@code null}
     */
    void jsonGetArray(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     */
    void jsonGet(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param paths the paths, must not be {@code null}. If no path are passed, this is equivalent to
     *        {@link #jsonGetObject(Object)},
     *        if multiple paths are passed, the produced JSON object contains the result (as a json array) for each path.
     */
    void jsonGet(K key, String... paths);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrappend/">JSON.ARRAPPEND</a>.
     * Summary: Append the json values into the array at path after the last element in it.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param values the values to append, encoded to JSON
     * @param <T> the type of value
     */
    <T> void jsonArrAppend(K key, String path, T... values);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrindex/">JSON.ARRINDEX</a>.
     * Summary: Searches for the first occurrence of a scalar JSON value in an array.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param value the value to be searched, encoded to JSON
     * @param start the start index
     * @param end the end index
     * @param <T> the type of value
     */
    <T> void jsonArrIndex(K key, String path, T value, int start, int end);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrindex/">JSON.ARRINDEX</a>.
     * Summary: Searches for the first occurrence of a scalar JSON value in an array.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param value the value to be searched, encoded to JSON
     * @param <T> the type of value
     */
    default <T> void jsonArrIndex(K key, String path, T value) {
        jsonArrIndex(key, path, value, 0, 0);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrinsert/">JSON.ARRINSERT</a>.
     * Summary: Inserts the json values into the array at path before the index (shifts to the right).
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param index the index. The index must be in the array's range. Inserting at index 0 prepends to the array.
     *        Negative index values start from the end of the array.
     * @param values the values to insert, encoded to JSON
     * @param <T> the type of value
     */
    <T> void jsonArrInsert(K key, String path, int index, T... values);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrlen/">JSON.ARRLEN</a>.
     * Summary: Reports the length of the JSON Array at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, {@code null} means {@code $}
     */
    void jsonArrLen(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrpop/">JSON.ARRPOP</a>.
     * Summary: Removes and returns an element from the index in the array.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param clazz the type of the popped object
     * @param path path the path, defaults to root if not provided.
     * @param index is the position in the array to start popping from (defaults to -1, meaning the last element).
     *        Out-of-range indexes round to their respective array ends.
     */
    <T> void jsonArrPop(K key, Class<T> clazz, String path, int index);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrpop/">JSON.ARRPOP</a>.
     * Summary: Removes and returns an element from the index in the array.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param clazz the type of the popped object
     * @param path path the path, defaults to root if not provided.
     **/
    default <T> void jsonArrPop(K key, Class<T> clazz, String path) {
        jsonArrPop(key, clazz, path, -1);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrtrim/">JSON.ARRTRIM</a>.
     * Summary: Trims an array so that it contains only the specified inclusive range of elements.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, must not be {@code null}
     * @param start the start index
     * @param stop the stop index
     */
    void jsonArrTrim(K key, String path, int start, int stop);

    /**
     * Execute the command <a href="https://redis.io/commands/json.clear/">JSON.CLEAR</a>.
     * Summary: Clears container values (Arrays/Objects), and sets numeric values to 0.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided. Non-existing paths are ignored.
     */
    void jsonClear(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.clear/">JSON.CLEAR</a>.
     * Summary: Clears container values (Arrays/Objects), and sets numeric values to 0.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     */
    default void jsonClear(K key) {
        jsonClear(key, null);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.del/">JSON.DEL</a>.
     * Summary: Deletes a value.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided. Non-existing paths are ignored.
     */
    void jsonDel(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.del/">JSON.DEL</a>.
     * Summary: Deletes a value.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     */
    default void jsonDel(K key) {
        jsonDel(key, null);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.mget/">JSON.MGET</a>.
     * Summary: Returns the values at path from multiple key arguments. Returns {@code null} for nonexistent keys
     * and nonexistent paths.
     * Group: json
     * <p>
     *
     * @param path path the path
     * @param keys the keys, must not be {@code null}, must not contain {@code null}
     */
    void jsonMget(String path, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/json.numincrby/">JSON.NUMINCRBY</a>.
     * Summary: Increments the number value stored at path by number.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided. Non-existing paths are ignored.
     * @param value the value to add
     */
    void jsonNumincrby(K key, String path, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/json.objkeys/">JSON.OBJKEYS</a>.
     * Summary: Returns the keys in the object that's referenced by path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     */
    void jsonObjKeys(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.objlen/">JSON.OBJLEN</a>.
     * Summary: Reports the number of keys in the JSON Object at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     */
    void jsonObjLen(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.strappend/">JSON.STRAPPEND</a>.
     * Summary: Appends the json-string values to the string at path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     * @param value the string to append, must not be {@code null}
     */
    void jsonStrAppend(K key, String path, String value);

    /**
     * Execute the command <a href="https://redis.io/commands/json.strlen/">JSON.STRLEN</a>.
     * Summary: Reports the length of the JSON String at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     */
    void jsonStrLen(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.toggle/">JSON.TOGGLE</a>.
     * Summary: Toggle a boolean value stored at path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, must not be {@code null}
     */
    void jsonToggle(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.type/">JSON.TYPE</a>.
     * Summary: Reports the type of JSON value at path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     */
    void jsonType(K key, String path);
}
