package io.quarkus.bootstrap.json;

public final class JsonMember implements JsonValue {
    private final String attribute;
    private final JsonValue value;

    public JsonMember(String attribute, JsonValue value) {
        this.attribute = attribute;
        this.value = value;
    }

    public JsonString attribute() {
        return new JsonString(attribute);
    }

    public String attributeName() {
        return attribute;
    }

    public JsonValue value() {
        return value;
    }
}
