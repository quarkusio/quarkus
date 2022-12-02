package io.quarkus.redis.datasource.codecs;

public interface Codec<T> {

    byte[] encode(T item);

    T decode(byte[] item);

}
