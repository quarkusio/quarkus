package io.quarkus.websockets.next;

/**
 * Mutable user data associated with a connection. Implementations must be thread-safe.
 */
public interface UserData {

    /**
     *
     * @param <VALUE>
     * @param key
     * @return the value or {@code null} if no mapping is found
     */
    <VALUE> VALUE get(TypedKey<VALUE> key);

    /**
     * Associates the specified value with the specified key. An old value is replaced by the specified value.
     *
     * @param <ConnectionData.VALUE>
     * @param key
     * @param value
     * @return the previous value associated with {@code key}, or {@code null} if no mapping exists
     */
    <VALUE> VALUE put(TypedKey<VALUE> key, VALUE value);

    /**
     *
     * @param <VALUE>
     * @param key
     */
    <VALUE> VALUE remove(TypedKey<VALUE> key);

    int size();

    void clear();

    /**
     * @param <TYPE> The type this key is used for.
     */
    record TypedKey<TYPE>(String value) {

        public static TypedKey<Integer> forInt(String key) {
            return new TypedKey<>(key);
        }

        public static TypedKey<Long> forLong(String key) {
            return new TypedKey<>(key);
        }

        public static TypedKey<String> forString(String key) {
            return new TypedKey<>(key);
        }

        public static TypedKey<Boolean> forBoolean(String key) {
            return new TypedKey<>(key);
        }
    }

}