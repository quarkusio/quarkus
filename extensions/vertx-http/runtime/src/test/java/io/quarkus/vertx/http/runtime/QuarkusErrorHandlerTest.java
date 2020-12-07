package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.Json;

class QuarkusErrorHandlerTest {

    @Test
    public void string_with_tab_should_be_correctly_escaped() {
        String initial = "String with a tab\tcharacter";
        String json = QuarkusErrorHandler.escapeJsonString(initial);
        String parsed = Json.decodeValue('"' + json + '"', String.class);
        assertEquals(initial, parsed);
    }

    @Test
    public void string_with_backslash_should_be_correctly_escaped() {
        String initial = "String with a backslash \\ character";
        String json = QuarkusErrorHandler.escapeJsonString(initial);
        String parsed = Json.decodeValue('"' + json + '"', String.class);
        assertEquals(initial, parsed);
    }

    @Test
    public void string_with_quotes_should_be_correctly_escaped() {
        String initial = "String with \"quoted text\"";
        String json = QuarkusErrorHandler.escapeJsonString(initial);
        String parsed = Json.decodeValue('"' + json + '"', String.class);
        assertEquals(initial, parsed);
    }

}
