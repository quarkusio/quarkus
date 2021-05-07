package io.quarkus.oidc;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class UserInfo {

    private JsonObject json;

    public UserInfo() {
    }

    public UserInfo(String userInfoJson) {
        json = toJsonObject(userInfoJson);
    }

    public UserInfo(JsonObject json) {
        this.json = json;
    }

    public String getString(String name) {
        return json.getString(name);
    }

    public JsonArray getArray(String name) {
        return json.getJsonArray(name);
    }

    public JsonObject getObject(String name) {
        return json.getJsonObject(name);
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

    private static JsonObject toJsonObject(String userInfoJson) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(userInfoJson))) {
            return jsonReader.readObject();
        }
    }
}
