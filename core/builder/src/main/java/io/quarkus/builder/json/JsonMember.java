package io.quarkus.builder.json;

public final class JsonMember implements JsonValue {
    private final JsonString attribute;
    private final JsonValue value;

    public JsonMember(JsonString attribute, JsonValue value) {
        this.attribute = attribute;
        this.value = value;
    }

    public JsonString attribute() {
        return attribute;
    }

    public JsonValue value() {
        return value;
    }
}
