package io.quarkus.devui.runtime.jsonrpc.json;

import java.util.Objects;
import java.util.function.Function;

public final class JsonTypeAdapter<T, S> {
    public final Class<T> type;
    public final Function<T, S> serializer;
    public final Function<S, T> deserializer;

    public JsonTypeAdapter(Class<T> type, Function<T, S> serializer, Function<S, T> deserializer) {
        this.type = Objects.requireNonNull(type, "type");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer");
    }
}
