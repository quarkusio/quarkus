package io.quarkus.test.security;

import java.io.StringReader;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonReader;

public enum AttributeType {
    LONG {
        @Override
        Object convert(String value) {
            return Long.valueOf(value);
        }
    },
    INTEGER {
        @Override
        Object convert(String value) {
            return Integer.valueOf(value);
        }
    },
    BOOLEAN {
        @Override
        Object convert(String value) {
            return Boolean.valueOf(value);
        }
    },
    STRING {
        @Override
        Object convert(String value) {
            return value;
        }
    },
    STRING_SET {
        /**
         * Returns a Set of String values, parsed from the given value.
         *
         * @param value a comma separated list of values
         */
        @Override
        Object convert(String value) {
            return Set.of(value.split(","));
        }
    },
    JSON_ARRAY {
        @Override
        Object convert(String value) {
            try (JsonReader reader = Json.createReader(new StringReader(value))) {
                return reader.readArray();
            }
        }
    },
    JSON_OBJECT {
        @Override
        Object convert(String value) {
            try (JsonReader reader = Json.createReader(new StringReader(value))) {
                return reader.readObject();
            }
        }
    },
    DEFAULT {
        @Override
        Object convert(String value) {
            return value;
        }
    };

    abstract Object convert(String value);
}
