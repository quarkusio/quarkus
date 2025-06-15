package io.quarkus.test.security.oidc;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public enum ClaimType {
    LONG {
        @Override
        public Long convert(String value) {
            return Long.parseLong(value);
        }
    },
    INTEGER {
        @Override
        public Integer convert(String value) {
            return Integer.parseInt(value);
        }
    },
    BOOLEAN {
        @Override
        public Boolean convert(String value) {
            return Boolean.parseBoolean(value);
        }
    },
    STRING {
        @Override
        public String convert(String value) {
            return value;
        }
    },
    JSON_ARRAY {
        @Override
        public JsonArray convert(String value) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(value))) {
                return jsonReader.readArray();
            }
        }
    },
    JSON_OBJECT {
        @Override
        public JsonObject convert(String value) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(value))) {
                return jsonReader.readObject();
            }
        }
    },
    DEFAULT {
        @Override
        public String convert(String value) {
            return value;
        }
    };

    abstract Object convert(String value);
}
