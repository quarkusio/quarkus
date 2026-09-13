package org.jboss.resteasy.reactive.common.headers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;

import jakarta.ws.rs.core.NewCookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class NewCookieHeaderDelegateTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Wed, 21 Oct 2015 07:28:00 GMT | 2015-10-21T07:28:00Z",
            "Mon, 11-Nov-24 22:59:57 GMT | 2024-11-11T22:59:57Z",
            "Sunday, 06-Nov-94 08:49:37 GMT | 1994-11-06T08:49:37Z",
            "Sun Nov  6 08:49:37 1994 | 1994-11-06T08:49:37Z",
            "Mon, 11-Nov-2024 22:59:57 GMT | 2024-11-11T22:59:57Z" })
    public void parsesExpiresFormats(String expires, String expectedInstant) {
        NewCookie cookie = parse("c1=v1; Expires=" + expires + "; Path=/; Domain=example.com");
        assertEquals(Date.from(Instant.parse(expectedInstant)), cookie.getExpiry());
        assertEquals("/", cookie.getPath());
        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    public void unparseableExpiresIsIgnored() {
        NewCookie cookie = parse("c1=v1; Expires=not-a-date; Path=/; Secure");
        assertNull(cookie.getExpiry());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isSecure());
    }

    @Test
    public void expiryRoundTrips() {
        NewCookie cookie = parse("c1=v1; Expires=Wed, 21 Oct 2015 07:28:00 GMT; Path=/");
        NewCookie reparsed = parse(NewCookieHeaderDelegate.INSTANCE.toString(cookie));
        assertEquals(cookie.getExpiry(), reparsed.getExpiry());
    }

    private static NewCookie parse(String setCookie) {
        return (NewCookie) NewCookieHeaderDelegate.INSTANCE.fromString(setCookie);
    }
}
