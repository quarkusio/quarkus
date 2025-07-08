package io.quarkus.vertx.http.runtime.cors;

import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isConfiguredWithWildcard;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isOriginAllowedByRegex;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isSameOrigin;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.parseAllowedOriginsRegex;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.substringMatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.vertx.core.http.HttpServerRequest;

public class CORSFilterTest {

    @Test
    public void isConfiguredWithWildcardTest() {
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.empty()));
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.of(Collections.EMPTY_LIST)));
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.of(Collections.singletonList("*"))));

        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Arrays.asList("PUT", "GET", "POST"))));
        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Arrays.asList("http://localhost:8080/", "*"))));
        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Collections.singletonList("http://localhost:8080/"))));
    }

    @Test
    public void isOriginAllowedByRegexTest() {
        Assertions.assertFalse(isOriginAllowedByRegex(Collections.emptyList(), "http://locahost:8080"));
        Assertions.assertEquals(
                parseAllowedOriginsRegex(Optional.of(Collections.singletonList("http://localhost:8080"))).size(),
                0);
        List<Pattern> regexList = parseAllowedOriginsRegex(
                Optional.of(Collections.singletonList("/https://([a-z0-9\\-_]+)\\.app\\.mydomain\\.com/")));
        Assertions.assertEquals(regexList.size(), 1);
        Assertions.assertTrue(isOriginAllowedByRegex(regexList, "https://abc-123.app.mydomain.com"));
        Assertions.assertFalse(isOriginAllowedByRegex(regexList, "https://abc-123app.mydomain.com"));
    }

    @Test
    public void sameOriginTest() {
        var request = Mockito.mock(HttpServerRequest.class);
        Mockito.when(request.scheme()).thenReturn("http");
        Mockito.when(request.host()).thenReturn("localhost");
        Mockito.when(request.absoluteURI()).thenReturn("http://localhost");
        Assertions.assertTrue(isSameOrigin(request, "http://localhost"));
        Assertions.assertTrue(isSameOrigin(request, "http://localhost:80"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:8080"));
        Assertions.assertFalse(isSameOrigin(request, "https://localhost"));
        Mockito.when(request.host()).thenReturn("localhost:8080");
        Mockito.when(request.absoluteURI()).thenReturn("http://localhost:8080");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:80"));
        Assertions.assertTrue(isSameOrigin(request, "http://localhost:8080"));
        Assertions.assertFalse(isSameOrigin(request, "https://localhost:8080"));
        Mockito.when(request.scheme()).thenReturn("https");
        Mockito.when(request.host()).thenReturn("localhost");
        Mockito.when(request.absoluteURI()).thenReturn("http://localhost");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:443"));
        Assertions.assertFalse(isSameOrigin(request, "https://localhost:8080"));
        Assertions.assertTrue(isSameOrigin(request, "https://localhost"));
        Mockito.when(request.host()).thenReturn("localhost:8443");
        Mockito.when(request.absoluteURI()).thenReturn("https://localhost:8443");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:80"));
        Assertions.assertFalse(isSameOrigin(request, "http://localhost:8443"));
        Assertions.assertTrue(isSameOrigin(request, "https://localhost:8443"));
        Assertions.assertFalse(isSameOrigin(request, "http://%s"));

    }

    @Test
    public void sameOriginPublicWebAddressTest() {
        var request = Mockito.mock(HttpServerRequest.class);
        Mockito.when(request.scheme()).thenReturn("https");
        Mockito.when(request.host()).thenReturn("stage.code.quarkus.io");
        Mockito.when(request.absoluteURI()).thenReturn("https://stage.code.quarkus.io/api/project");
        Assertions.assertFalse(isSameOrigin(request, "http://localhost"));
        Assertions.assertFalse(isSameOrigin(request, "https://code.quarkus.io"));
        Assertions.assertTrue(isSameOrigin(request, "https://stage.code.quarkus.io"));
    }

    @Test
    public void testSubstringMatches() {
        Assertions.assertTrue(substringMatch("localhost", 0, "local", false));
        Assertions.assertFalse(substringMatch("localhost", 0, "local", true));
        Assertions.assertFalse(substringMatch("localhost", 1, "local", false));
        Assertions.assertTrue(substringMatch("localhost", 5, "host", false));
        Assertions.assertTrue(substringMatch("localhost", 5, "host", true));

    }
}
