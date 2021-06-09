package io.quarkus.vertx.http.runtime.cors;

import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isConfiguredWithWildcard;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isOriginAllowedByRegex;
import static io.quarkus.vertx.http.runtime.cors.CORSFilter.parseAllowedOriginsRegex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
    }
}
