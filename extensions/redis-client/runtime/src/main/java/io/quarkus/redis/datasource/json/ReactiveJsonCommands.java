package io.quarkus.redis.datasource.json;

import java.util.Collections;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Allows executing commands from the {@code json} group (requires the Redis stack).
 * See <a href="https://redis.io/commands/?group=json">the json command list</a> for further information about these commands.
 * <p>
 * Redis JSON lets you store, update, and retrieve JSON values in a Redis database, similar to any other Redis data type.
 * <p>
 * Ths API is based on the {@link JsonObject} and {@link JsonArray} types.
 * Some methods also allows direct <em>mapping</em> from and to objects. In the case of deserialization, the
 * {@link Class Class<T>} must be passed.
 *
 * @param <K> the type of the key
 */
public interface ReactiveJsonCommands<K> extends ReactiveRedisCommands {

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param value the value, encoded to JSON
     * @param <T> the type for the value
     * @return A uni emitting {@code null} when the operation completes
     **/
    <T> Uni<Void> jsonSet(K key, String path, T value);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON object to store, must not be {@code null}
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> jsonSet(K key, String path, JsonObject json);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON object to store, must not be {@code null}
     * @param args the extra arguments
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> jsonSet(K key, String path, JsonObject json, JsonSetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON array to store, must not be {@code null}
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> jsonSet(K key, String path, JsonArray json);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param json the JSON array to store, must not be {@code null}
     * @param args the extra arguments
     * @return A uni emitting {@code null} when the operation completes
     **/
    Uni<Void> jsonSet(K key, String path, JsonArray json, JsonSetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/json.set/">JSON.SET</a>.
     * Summary: Sets the JSON value at path in key.
     * Group: json
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @param value the value to store, encoded to JSON.
     * @param args the extra arguments
     * @return A uni emitting {@code null} when the operation completes
     **/
    <T> Uni<Void> jsonSet(K key, String path, T value, JsonSetArgs args);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     * This method uses the root path ({@code $}).
     * It map the retrieve JSON document to an object of type {@code <T>}.
     *
     * @param key the key, must not be {@code null}
     * @param clazz the type of object to recreate from the JSON content
     * @return a uni emitting the object, {@code null} if it does not exist
     **/
    <T> Uni<T> jsonGet(K key, Class<T> clazz);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     * This method uses the root path ({@code $}).
     * Unlike {@link #jsonGet(Object, Class)}, it returns a {@link JsonObject}.
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting the stored JSON object, {@code null} if it does not exist
     **/
    Uni<JsonObject> jsonGetObject(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     * This method uses the root path ({@code $}).
     * Unlike {@link #jsonGet(Object, Class)}, it returns a {@link JsonArray}.
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting the stored JSON array, {@code null} if it does not exist
     **/
    Uni<JsonArray> jsonGetArray(K key);

    /**
     * Execute the command <a href="https://redis.io/commands/json.get/">JSON.GET</a>.
     * Summary: Returns the value at path in JSON serialized form.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, must not be {@code null}
     * @return a uni emitting the JSON array containing the different results, {@code null} if it does not exist.
     **/
    Uni<JsonArray> jsonGet(K key, String path);

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
     * @return a uni emitting the stored JSON object, {@code null} if it does not exist.
     *         If no path are passed, this is equivalent to {@link #jsonGetObject(Object)}.
     *         If multiple paths are passed, the produced JSON object contains the result for each pass as a JSON array.
     **/
    Uni<JsonObject> jsonGet(K key, String... paths);

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
     * @return a uni emitting a list with the new sizes of each modified array (in order) or {@code null} (instead of
     *         the size) if the point object was not an array.
     **/
    <T> Uni<List<Integer>> jsonArrAppend(K key, String path, T... values);

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
     * @return a uni emitting a list with the first position in the array of each JSON value that matches the path,
     *         {@code -1} if not found in the array, or {@code null} if the matching JSON value is not an array.
     **/
    <T> Uni<List<Integer>> jsonArrIndex(K key, String path, T value, int start, int end);

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
     * @return a uni emitting a list with the first position in the array of each JSON value that matches the path,
     *         {@code -1} if not found in the array, or {@code null} if the matching JSON value is not an array.
     **/
    default <T> Uni<List<Integer>> jsonArrIndex(K key, String path, T value) {
        return jsonArrIndex(key, path, value, 0, 0);
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
     * @return a uni emitting a list of integer containing for each path, the array's new size or {@code null} if the
     *         matching JSON value is not an array.
     **/
    <T> Uni<List<Integer>> jsonArrInsert(K key, String path, int index, T... values);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrlen/">JSON.ARRLEN</a>.
     * Summary: Reports the length of the JSON Array at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path the path, {@code null} means {@code $}
     * @return a uni emitting a list of integer containing for each path, the array's length, or {@code null} if the
     *         matching JSON value is not an array.
     **/
    Uni<List<Integer>> jsonArrLen(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrlen/">JSON.ARRLEN</a>.
     * Summary: Reports the length of the JSON Array at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting the array's length, or {@code null} if the matching JSON value is not an array.
     **/
    default Uni<Integer> jsonArrLen(K key) {
        return jsonArrLen(key, null).map(l -> l.get(0));
    }

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
     * @return a uni emitting a list of T including for each path, an instance of T rebuilt from the JSON value, or
     *         {@code null} if the matching JSON value is not an array. Popping an empty array produces {@code null}.
     **/
    <T> Uni<List<T>> jsonArrPop(K key, Class<T> clazz, String path, int index);

    /**
     * Execute the command <a href="https://redis.io/commands/json.arrpop/">JSON.ARRPOP</a>.
     * Summary: Removes and returns an element from the index in the array.
     * Group: json
     * <p>
     * <p>
     * This variant popped from the root object (must be a JSON array), and returns a single item.
     *
     * @param key the key, must not be {@code null}
     * @param clazz the type of the popped object
     * @return a uni emitting an instance of T.
     **/
    default <T> Uni<T> jsonArrPop(K key, Class<T> clazz) {
        return jsonArrPop(key, clazz, null, -1).map(l -> l.get(0));
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
     * @return a uni emitting a list of integer containing, for each path, the array's new size, or {@code null} if
     *         the matching JSON value is not an array.
     **/
    Uni<List<Integer>> jsonArrTrim(K key, String path, int start, int stop);

    /**
     * Execute the command <a href="https://redis.io/commands/json.clear/">JSON.CLEAR</a>.
     * Summary: Clears container values (Arrays/Objects), and sets numeric values to 0.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided. Non-existing paths are ignored.
     * @return a uni emitting the number of value cleared
     **/
    Uni<Integer> jsonClear(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.clear/">JSON.CLEAR</a>.
     * Summary: Clears container values (Arrays/Objects), and sets numeric values to 0.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting the number of value cleared
     **/
    default Uni<Integer> jsonClear(K key) {
        return jsonClear(key, null);
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.del/">JSON.DEL</a>.
     * Summary: Deletes a value.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided. Non-existing paths are ignored.
     * @return a uni emitting the number of path deleted
     **/
    Uni<Integer> jsonDel(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.del/">JSON.DEL</a>.
     * Summary: Deletes a value.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting the number of path deleted (0 or more)
     **/
    default Uni<Integer> jsonDel(K key) {
        return jsonDel(key, null);
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
     * @return a uni emitting a list of JsonArray containing each retrieved value
     **/
    Uni<List<JsonArray>> jsonMget(String path, K... keys);

    /**
     * Execute the command <a href="https://redis.io/commands/json.numincrby/">JSON.NUMINCRBY</a>.
     * Summary: Increments the number value stored at path by number.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided. Non-existing paths are ignored.
     * @param value the value to add
     * @return a uni emitting {@code null} when the operation completes
     **/
    Uni<Void> jsonNumincrby(K key, String path, double value);

    /**
     * Execute the command <a href="https://redis.io/commands/json.objkeys/">JSON.OBJKEYS</a>.
     * Summary: Returns the keys in the object that's referenced by path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     * @return a uni emitting a list containing, for each matching path, the list of keys, or {@code null} if the
     *         matching JSON value is not an object.
     **/
    Uni<List<List<String>>> jsonObjKeys(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.objkeys/">JSON.OBJKEYS</a>.
     * Summary: Returns the keys in the object that's referenced by path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting a list containing the list of keys, or {@code null} if the stored value is not a JSON object.
     **/
    default Uni<List<String>> jsonObjKeys(K key) {
        return jsonObjKeys(key, null).map(l -> {
            if (l != null && !l.isEmpty()) {
                return l.get(0);
            }
            return Collections.emptyList();
        });
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.objlen/">JSON.OBJLEN</a>.
     * Summary: Reports the number of keys in the JSON Object at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     * @return a uni emitting a list containing, for each path, the length of the object, {@code null} if the matching
     *         JSON value is not an object
     **/
    Uni<List<Integer>> jsonObjLen(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.objlen/">JSON.OBJLEN</a>.
     * Summary: Reports the number of keys in the JSON Object at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @return a uni emitting the length of the JSON object, {@code null} if the matching JSON value is not an object
     **/
    default Uni<Integer> jsonObjLen(K key) {
        return jsonObjLen(key, null)
                .map(l -> l.get(0));
    }

    /**
     * Execute the command <a href="https://redis.io/commands/json.strappend/">JSON.STRAPPEND</a>.
     * Summary: Appends the json-string values to the string at path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     * @param value the string to append, must not be {@code null}
     * @return a uni emitting a list containing, for each path, the new string length, {@code null} if the matching JSON
     *         value is not a string.
     **/
    Uni<List<Integer>> jsonStrAppend(K key, String path, String value);

    /**
     * Execute the command <a href="https://redis.io/commands/json.strlen/">JSON.STRLEN</a>.
     * Summary: Reports the length of the JSON String at path in key.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     * @return a uni emitting a list containing, for each path, the length of the string, {@code null} if the matching JSON
     *         value is not a string. Returns {@code null} if the key or path do not exist.
     **/
    Uni<List<Integer>> jsonStrLen(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.toggle/">JSON.TOGGLE</a>.
     * Summary: Toggle a boolean value stored at path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, must not be {@code null}
     * @return a uni emitting a list containing, for each path, the new boolean value, {@code null} if the matching JSON
     *         value is not a boolean.
     **/
    Uni<List<Boolean>> jsonToggle(K key, String path);

    /**
     * Execute the command <a href="https://redis.io/commands/json.type/">JSON.TYPE</a>.
     * Summary: Reports the type of JSON value at path.
     * Group: json
     * <p>
     *
     * @param key the key, must not be {@code null}
     * @param path path the path, path defaults to {@code $} if not provided.
     * @return a uni emitting a list containing, for each path, the json type as String (string, integer, number, boolean,
     *         object, array), empty if no match.
     **/
    Uni<List<String>> jsonType(K key, String path);

}
