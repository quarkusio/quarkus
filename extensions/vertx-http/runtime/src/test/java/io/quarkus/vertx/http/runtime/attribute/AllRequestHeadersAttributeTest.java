package io.quarkus.vertx.http.runtime.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

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
        assertEquals("Authorization: Bearer <hidden>", attribute);
    }

    @Test
    void testAuthorizationSchemeValueMasked() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Authorization", "DPoP token");
        String attribute = new AllRequestHeadersAttribute().readAttribute(headers);
        assertEquals("Authorization: DPoP <hidden>", attribute);
    }

    @Test
    void testAuthorizationValueMasked() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("authorization", "token");
        String attribute = new AllRequestHeadersAttribute().readAttribute(headers);
        assertEquals("authorization: <hidden>", attribute);
    }

    @Test
    void testMaskedCookieWhenFirst() {
        AllRequestHeadersAttribute attr = new AllRequestHeadersAttribute(
                Set.of(), Set.of("Session"));

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Cookie", "Session=encrypted; visitcount=1");

        assertEquals("Cookie: Session=<hidden>; visitcount=1", attr.readAttribute(headers));
    }

    @Test
    void testMaskedCookieWhenNotFirst() {
        AllRequestHeadersAttribute attr = new AllRequestHeadersAttribute(
                Set.of(), Set.of("Session"));

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Cookie", "visitcount=1; Session=encrypted");

        assertEquals("Cookie: visitcount=1; Session=<hidden>", attr.readAttribute(headers));
    }

    @Test
    void testMultipleMaskedCookies() {
        AllRequestHeadersAttribute attr = new AllRequestHeadersAttribute(
                Set.of(), Set.of("Session", "Token"));

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Cookie", "visitcount=1; Session=encrypted; Token=secret");

        assertEquals("Cookie: visitcount=1; Session=<hidden>; Token=<hidden>", attr.readAttribute(headers));
    }

    @Test
    void testNoMaskedCookiesConfigured() {
        AllRequestHeadersAttribute attr = new AllRequestHeadersAttribute(
                Set.of(), Set.of());

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Cookie", "Session=encrypted; visitcount=1");

        assertEquals("Cookie: Session=encrypted; visitcount=1", attr.readAttribute(headers));
    }

}
