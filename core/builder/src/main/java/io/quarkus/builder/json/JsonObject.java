package io.quarkus.builder.json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.builder.JsonTransform;

public final class JsonObject implements JsonMultiValue {
    private final Map<JsonString, JsonValue> value;

    public JsonObject(Map<JsonString, JsonValue> value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T get(String attribute) {
        return (T) value.get(new JsonString(attribute));
    }

    public List<JsonMember> members() {
        return value.entrySet().stream()
                .map(e -> new JsonMember(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void forEach(JsonTransform transform) {
        members().forEach(member -> transform.accept(null, member));
    }
}
