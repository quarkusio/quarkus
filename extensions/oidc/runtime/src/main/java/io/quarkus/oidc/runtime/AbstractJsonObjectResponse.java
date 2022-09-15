package io.quarkus.oidc.runtime;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class AbstractJsonObjectResponse {
    private String jsonString;
    private JsonObject json;

    public AbstractJsonObjectResponse() {
    }

    public AbstractJsonObjectResponse(String jsonString) {
        this(toJsonObject(jsonString));
        this.jsonString = jsonString;
    }

    public AbstractJsonObjectResponse(JsonObject json) {
        this.json = json;
    }

    public String getString(String name) {
        return json.getString(name);
    }

    public Boolean getBoolean(String name) {
        return json.getBoolean(name);
    }

    public Long getLong(String name) {
        JsonNumber number = json.getJsonNumber(name);
        return number != null ? number.longValue() : null;
    }

    public JsonArray getArray(String name) {
        return json.getJsonArray(name);
    }

    public JsonObject getObject(String name) {
        return json.getJsonObject(name);
    }

    public JsonObject getJsonObject() {
        return json;
    }

    public Object get(String name) {
        return json.get(name);
    }

    public boolean contains(String propertyName) {
        return json.containsKey(propertyName);
    }

    public Set<String> getPropertyNames() {
        return Collections.unmodifiableSet(json.keySet());
    }

    public Set<Map.Entry<String, JsonValue>> getAllProperties() {
        return Collections.unmodifiableSet(json.entrySet());
    }

    protected String getNonNullJsonString() {
        return jsonString == null ? json.toString() : jsonString;
    }

    private static JsonObject toJsonObject(String userInfoJson) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(userInfoJson))) {
            return jsonReader.readObject();
        }
    }
}
