package io.quarkus.oidc.common.runtime;

import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public abstract class AbstractJsonObject {
    private String jsonString;
    private JsonObject json;

    protected AbstractJsonObject() {
        json = jsonProvider().createObjectBuilder().build();
    }

    protected AbstractJsonObject(String jsonString) {
        this(toJsonObject(jsonString));
        this.jsonString = jsonString;
    }

    protected AbstractJsonObject(JsonObject json) {
        this.json = json;
    }

    public String getString(String name) {
        return contains(name) ? json.getString(name) : null;
    }

    public Boolean getBoolean(String name) {
        return contains(name) ? json.getBoolean(name) : null;
    }

    public Long getLong(String name) {
        JsonNumber number = contains(name) ? json.getJsonNumber(name) : null;
        return number != null ? number.longValue() : null;
    }

    public JsonArray getArray(String name) {
        return contains(name) ? json.getJsonArray(name) : null;
    }

    public JsonObject getObject(String name) {
        return contains(name) ? json.getJsonObject(name) : null;
    }

    public JsonObject getJsonObject() {
        return jsonProvider().createObjectBuilder(json).build();
    }

    public Object get(String name) {
        return json.get(name);
    }

    public boolean contains(String propertyName) {
        return json != null && json.containsKey(propertyName) && !json.isNull(propertyName);
    }

    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(json.keySet());
    }

    public Set<Map.Entry<String, JsonValue>> getAllProperties() {
        return Collections.unmodifiableSet(json.entrySet());
    }

    protected String getJsonString() {
        return jsonString == null ? json.toString() : jsonString;
    }

    protected List<String> getListOfStrings(String prop) {
        JsonArray array = getArray(prop);
        if (array == null) {
            return null;
        }
        List<String> list = new ArrayList<String>();
        for (JsonValue value : array) {
            list.add(((JsonString) value).getString());
        }

        return list;
    }

    public static JsonObject toJsonObject(String json) {
        try (JsonReader jsonReader = jsonProvider().createReader(new StringReader(json))) {
            return jsonReader.readObject();
        }
    }
}
