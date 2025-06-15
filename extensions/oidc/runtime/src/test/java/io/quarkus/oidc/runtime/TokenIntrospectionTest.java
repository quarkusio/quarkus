package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.TokenIntrospection;

public class TokenIntrospectionTest {
    TokenIntrospection introspection = new TokenIntrospection("{" + "\"active\": true," + "\"username\": \"alice\","
            + "\"sub\": \"1234567\"," + "\"aud\": \"http://localhost:8080\"," + "\"iss\": \"http://keycloak/realm\","
            + "\"client_id\": \"quarkus\"," + "\"custom\": null," + "\"id\": 1234,"
            + "\"permissions\": [\"read\", \"write\"]," + "\"scope\": \"add divide\","
            + "\"scopes\": {\"scope\": \"see\"}" + "}");

    @Test
    public void testActive() {
        assertTrue(introspection.isActive());
    }

    @Test
    public void testGetUsername() {
        assertEquals("alice", introspection.getUsername());
    }

    @Test
    public void testGetSubject() {
        assertEquals("1234567", introspection.getSubject());
    }

    @Test
    public void testGetAudience() {
        assertEquals("http://localhost:8080", introspection.getAudience());
    }

    @Test
    public void testGetIssuer() {
        assertEquals("http://keycloak/realm", introspection.getIssuer());
    }

    @Test
    public void testGetScopes() {
        assertEquals(Set.of("add", "divide"), introspection.getScopes());
    }

    @Test
    public void testGetClientId() {
        assertEquals("quarkus", introspection.getClientId());
    }

    @Test
    public void testGetString() {
        assertEquals("alice", introspection.getString("username"));
        assertNull(introspection.getString("usernames"));
    }

    @Test
    public void testGetBoolean() {
        assertTrue(introspection.getBoolean("active"));
        assertNull(introspection.getBoolean("activate"));
    }

    @Test
    public void testGetLong() {
        assertEquals(1234, introspection.getLong("id"));
        assertNull(introspection.getLong("ids"));
    }

    @Test
    public void testGetArray() {
        JsonArray array = introspection.getArray("permissions");
        assertNotNull(array);
        assertEquals(2, array.size());
        assertEquals("read", array.getString(0));
        assertEquals("write", array.getString(1));
        assertNull(introspection.getArray("permit"));
    }

    @Test
    public void testGetObject() {
        JsonObject map = introspection.getObject("scopes");
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("see", map.getString("scope"));
    }

    @Test
    public void testGetNullProperty() {
        assertNull(introspection.getString("custom"));
    }
}
