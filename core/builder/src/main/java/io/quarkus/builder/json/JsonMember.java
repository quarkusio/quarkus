package io.quarkus.builder.json;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonMember}
 */
@Deprecated(forRemoval = true)
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
