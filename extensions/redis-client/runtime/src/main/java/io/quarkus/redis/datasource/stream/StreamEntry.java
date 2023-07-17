package io.quarkus.redis.datasource.stream;

public class StreamEntry<T> {

    public final String id;

    public final T content;

    public StreamEntry(String id, T content) {
        this.id = id;
        this.content = content;
    }

}
