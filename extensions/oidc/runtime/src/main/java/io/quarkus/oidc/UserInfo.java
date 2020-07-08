package io.quarkus.oidc;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class UserInfo {

    private JsonObject json;

    public UserInfo() {
    }

    public UserInfo(String userInfoJson) {
        json = toJsonObject(userInfoJson);
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

    private static JsonObject toJsonObject(String userInfoJson) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(userInfoJson))) {
            return jsonReader.readObject();
        }
    }
}
