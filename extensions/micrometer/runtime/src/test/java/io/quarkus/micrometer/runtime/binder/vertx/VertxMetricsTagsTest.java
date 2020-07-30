package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.micrometer.core.instrument.Tag;
import io.vertx.core.http.HttpServerResponse;

/**
 * Test tag creation
 */
@DisabledOnJre(JRE.JAVA_8)
public class VertxMetricsTagsTest {

    @Mock
    HttpServerResponse response;

    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final List<Pattern> NO_MATCH_PATTERNS = Collections.emptyList();

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testParsePathNoIgnorePatterns() {
        Assertions.assertEquals("/", VertxMetricsTags.parseUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "//"));
        Assertions.assertEquals("/", VertxMetricsTags.parseUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, ""));
        Assertions.assertEquals("/path/with/no/leading/slash",
                VertxMetricsTags.parseUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "path/with/no/leading/slash"));
        Assertions.assertEquals("/path/with/query/string",
                VertxMetricsTags.parseUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/path/with/query/string?stuff"));
    }

    @Test
    public void testParsePathWithIgnorePatterns() {
        List<Pattern> ignorePatterns = Arrays.asList(Pattern.compile("/ignore.*"));

        Assertions.assertNull(
                VertxMetricsTags.parseUriPath(NO_MATCH_PATTERNS, ignorePatterns, "ignore/me/with/no/leading/slash"));
        Assertions.assertNull(
                VertxMetricsTags.parseUriPath(NO_MATCH_PATTERNS, ignorePatterns, "/ignore/me/with/query/string?stuff"));
    }

    @Test
    public void testStatus() {
        Assertions.assertEquals(Tag.of("status", "200"), VertxMetricsTags.status(200));
        Assertions.assertEquals(Tag.of("status", "301"), VertxMetricsTags.status(301));
        Assertions.assertEquals(Tag.of("status", "304"), VertxMetricsTags.status(304));
        Assertions.assertEquals(Tag.of("status", "404"), VertxMetricsTags.status(404));
    }

    @Test
    public void testUriRedirect() {
        Assertions.assertEquals(VertxMetricsTags.URI_REDIRECTION, VertxMetricsTags.uri("/moved", 301));
        Assertions.assertEquals(VertxMetricsTags.URI_REDIRECTION, VertxMetricsTags.uri("/moved", 302));
        Assertions.assertEquals(VertxMetricsTags.URI_REDIRECTION, VertxMetricsTags.uri("/moved", 304));
    }

    @Test
    public void testUriDefaults() {
        Assertions.assertEquals(VertxMetricsTags.URI_ROOT, VertxMetricsTags.uri("/", 200));
        Assertions.assertEquals(Tag.of("uri", "/known/ok"), VertxMetricsTags.uri("/known/ok", 200));
        Assertions.assertEquals(VertxMetricsTags.URI_NOT_FOUND, VertxMetricsTags.uri("/invalid", 404));
        Assertions.assertEquals(Tag.of("uri", "/known/bad/request"), VertxMetricsTags.uri("/known/bad/request", 400));
        Assertions.assertEquals(Tag.of("uri", "/known/server/error"), VertxMetricsTags.uri("/known/server/error", 500));
    }
}
