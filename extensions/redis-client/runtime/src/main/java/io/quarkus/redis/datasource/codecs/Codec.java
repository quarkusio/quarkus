package io.quarkus.redis.datasource.codecs;

import java.lang.reflect.Type;

/**
 * Redis codec interface.
 * <p>
 * The Redis data source uses this interface to serialize and deserialize data to and from Redis. A set of default
 * codecs are provided for Strings, Integers, Doubles and Byte arrays. For custom types, either there is a specific
 * implementation of this interface exposed as CDI (Application Scoped) bean, or JSON is used.
 */
public interface Codec {

    /**
     * Checks if the current codec can handle the serialization and deserialization of object from the given type.
     *
     * @param clazz
     *        the type, cannot be {@code null}
     *
     * @return {@code true} if the codec can handle the type, {@code false} otherwise
     */
    boolean canHandle(Type clazz);

    /**
     * Encodes the given object. The type of the given object matches the type used to call the {@link #canHandle(Type)}
     * method.
     *
     * @param item
     *        the item
     *
     * @return the encoded content
     */
    byte[] encode(Object item);

    /**
     * Decodes the given bytes to an object. The codec must return an instance of the type used to call the
     * {@link #canHandle(Type)} method.
     *
     * @param item
     *        the bytes
     *
     * @return the object
     */
    Object decode(byte[] item);

}
