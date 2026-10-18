package io.quarkus.bootstrap.json;

public final class JsonInteger implements JsonNumber {
    private final long value;

    public JsonInteger(long value) {
        this.value = value;
    }

    public long longValue() {
        return value;
    }

    public int intValue() {
        return (int) value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
