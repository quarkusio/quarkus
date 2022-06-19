package io.quarkus.redis.datasource.api.codecs;

public interface Codec<T> {

    byte[] encode(T item);

    T decode(byte[] item);

}
