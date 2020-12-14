package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.micrometer.core.instrument.Tag;
import io.quarkus.micrometer.runtime.binder.HttpTags;
import io.vertx.core.http.HttpServerResponse;

/**
 * Test tag creation
 * Disabled on Java 8 because of Mocks
 */
@DisabledOnJre(JRE.JAVA_8)
public class VertxMetricsTagsTest {

    @Mock
    HttpServerResponse response;

    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final List<Pattern> ignorePatterns = Arrays.asList(Pattern.compile("/ignore.*"));

    final Map<Pattern, String> NO_MATCH_PATTERNS = Collections.emptyMap();

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testParsePathDoubleSlash() {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "//");
        Assertions.assertEquals("/", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure);
        Assertions.assertFalse(requestMetric.pathMatched);
    }

    @Test
    public void testParseEmptyPath() {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "");
        Assertions.assertTrue(requestMetric.measure);
        Assertions.assertFalse(requestMetric.pathMatched);
        Assertions.assertEquals("/", requestMetric.path);
    }

    @Test
    public void testParsePathNoLeadingSlash() {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "path/with/no/leading/slash");
        Assertions.assertEquals("/path/with/no/leading/slash", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure);
        Assertions.assertFalse(requestMetric.pathMatched);
    }

    @Test
    public void testParsePathWithQueryString() {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/path/with/query/string?stuff");
        Assertions.assertEquals("/path/with/query/string", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure);
        Assertions.assertFalse(requestMetric.pathMatched);
    }

    @Test
    public void testParsePathIgnoreNoLeadingSlash() {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, NO_MATCH_PATTERNS, ignorePatterns, "ignore/me/with/no/leading/slash");
        Assertions.assertEquals("/ignore/me/with/no/leading/slash", requestMetric.path);
        Assertions.assertFalse(requestMetric.measure);
        Assertions.assertFalse(requestMetric.pathMatched);
    }

    @Test
    public void testParsePathIgnoreWithQueryString() {
        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, NO_MATCH_PATTERNS, ignorePatterns, "/ignore/me/with/query/string?stuff");
        Assertions.assertEquals("/ignore/me/with/query/string", requestMetric.path);
        Assertions.assertFalse(requestMetric.measure);
        Assertions.assertFalse(requestMetric.pathMatched);
    }

    @Test
    public void testParsePathMatchReplaceNoLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, matchPatterns, NO_IGNORE_PATTERNS, "item/123");
        Assertions.assertEquals("/item/{id}", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure);
        Assertions.assertTrue(requestMetric.pathMatched);
    }

    @Test
    public void testParsePathMatchReplaceLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        RequestMetric requestMetric = new RequestMetric();
        VertxMetricsTags.parseUriPath(requestMetric, matchPatterns, NO_IGNORE_PATTERNS, "/item/123");
        Assertions.assertEquals("/item/{id}", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure);
        Assertions.assertTrue(requestMetric.pathMatched);
    }

    @Test
    public void testStatus() {
        Assertions.assertEquals(Tag.of("status", "200"), HttpTags.status(200));
        Assertions.assertEquals(Tag.of("status", "301"), HttpTags.status(301));
        Assertions.assertEquals(Tag.of("status", "304"), HttpTags.status(304));
        Assertions.assertEquals(Tag.of("status", "404"), HttpTags.status(404));
    }

    @Test
    public void testUriRedirect() {
        Assertions.assertEquals(HttpTags.URI_REDIRECTION, HttpTags.uri("/moved", 301));
        Assertions.assertEquals(HttpTags.URI_REDIRECTION, HttpTags.uri("/moved", 302));
        Assertions.assertEquals(HttpTags.URI_REDIRECTION, HttpTags.uri("/moved", 304));
    }

    @Test
    public void testUriDefaults() {
        Assertions.assertEquals(HttpTags.URI_ROOT, HttpTags.uri("/", 200));
        Assertions.assertEquals(Tag.of("uri", "/known/ok"), HttpTags.uri("/known/ok", 200));
        Assertions.assertEquals(HttpTags.URI_NOT_FOUND, HttpTags.uri("/invalid", 404));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/request"), HttpTags.uri("/known/bad/request", 400));
        Assertions.assertEquals(Tag.of("uri", "/known/server/error"), HttpTags.uri("/known/server/error", 500));
    }
}
