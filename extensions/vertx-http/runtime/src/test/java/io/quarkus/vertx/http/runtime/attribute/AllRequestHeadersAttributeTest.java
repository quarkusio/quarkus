package io.quarkus.vertx.http.runtime.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.vertx.core.MultiMap;

class AllRequestHeadersAttributeTest {

    @Test
    void testHeaderValueNotMasked() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Content-Type", "application/json");
        String attribute = new AllRequestHeadersAttribute().readAttribute(headers);
        assertEquals("Content-Type: application/json", attribute);
    }

    @Test
    void testAuthorizationBearerValueMasked() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Authorization", "Bearer token");
        String attribute = new AllRequestHeadersAttribute().readAttribute(headers);
        assertEquals("Authorization: Bearer ...", attribute);
    }

    @Test
    void testAuthorizationSchemeValueMasked() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Authorization", "DPoP token");
        String attribute = new AllRequestHeadersAttribute().readAttribute(headers);
        assertEquals("Authorization: DPoP ...", attribute);
    }

    @Test
    void testAuthorizationValueMasked() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("authorization", "token");
        String attribute = new AllRequestHeadersAttribute().readAttribute(headers);
        assertEquals("authorization: ...", attribute);
    }

}
