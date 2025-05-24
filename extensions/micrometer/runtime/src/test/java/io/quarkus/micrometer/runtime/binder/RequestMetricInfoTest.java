package io.quarkus.micrometer.runtime.binder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;

public class RequestMetricInfoTest {

    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final List<Pattern> ignorePatterns = Arrays.asList(Pattern.compile("/ignore.*"));

    final Map<Pattern, String> NO_MATCH_PATTERNS = Collections.emptyMap();
    RequestMetricInfo requestMetric;

    @BeforeEach
    public void init() {
        requestMetric = new RequestMetricInfo();
    }

    @Test
    public void testParsePathSingleSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        Assertions.assertEquals("/", path);
    }

    @Test
    public void testParsePathDoubleSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "//");
        Assertions.assertEquals("/", path);
    }

    @Test
    public void testParsePathMultipleSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/////");
        Assertions.assertEquals("/", path);
    }

    @Test
    public void testParseEmptyPath() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "");
        Assertions.assertEquals("/", path);
    }

    @Test
    public void testParseNullPath() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, null);
        Assertions.assertEquals("/", path);
    }

    @Test
    public void testParsePathNoLeadingSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "path/with/no/leading/slash");
        Assertions.assertEquals("/path/with/no/leading/slash", path);
    }

    @Test
    public void testParsePathNoEndSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "/path/with/no/end/slash/");
        Assertions.assertEquals("/path/with/no/end/slash", path);
    }

    @Test
    public void testParsePathNoEndDoubleSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "/path/with/no/end/double/slash///////");
        Assertions.assertEquals("/path/with/no/end/double/slash", path);
    }

    @Test
    public void testParsePathIgnoreNoLeadingSlash() {
        String path = requestMetric.getNormalizedUriPath(NO_MATCH_PATTERNS, ignorePatterns,
                "ignore/me/with/no/leading/slash");
        Assertions.assertEquals(null, path);
    }

    @Test
    public void testHttpServerMetricsIgnorePatterns() {
        HttpServerConfig httpServerConfig = Mockito.mock(HttpServerConfig.class);
        Mockito.doReturn(Optional.of(new ArrayList<>(Arrays.asList(" /item/.* ", " /oranges/.* "))))
                .when(httpServerConfig).ignorePatterns();
        HttpClientConfig httpClientConfig = Mockito.mock(HttpClientConfig.class);
        VertxConfig vertxConfig = Mockito.mock(VertxConfig.class);
        HttpBinderConfiguration binderConfig = new HttpBinderConfiguration(
                true, false,
                httpServerConfig, httpClientConfig, vertxConfig);
        Assertions.assertEquals(2, binderConfig.serverIgnorePatterns.size());

        Pattern p = binderConfig.serverIgnorePatterns.get(0);
        Assertions.assertEquals("/item/.*", p.pattern());
        Assertions.assertTrue(p.matcher("/item/123").matches());

        p = binderConfig.serverIgnorePatterns.get(1);
        Assertions.assertEquals("/oranges/.*", p.pattern());
        Assertions.assertTrue(p.matcher("/oranges/123").matches());
    }

    @Test
    public void testParsePathMatchReplaceNoLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        String path = requestMetric.getNormalizedUriPath(matchPatterns, NO_IGNORE_PATTERNS, "item/123");
        Assertions.assertEquals("/item/{id}", path);
    }

    @Test
    public void testParsePathMatchReplaceLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        String path = requestMetric.getNormalizedUriPath(matchPatterns, NO_IGNORE_PATTERNS, "/item/123");
        Assertions.assertEquals("/item/{id}", path);
    }

    @Test
    public void testHttpServerMetricsMatchPatterns() {

        HttpServerConfig httpServerConfig = Mockito.mock(HttpServerConfig.class);
        Mockito.doReturn(Optional
                .of(new ArrayList<>(Arrays.asList(" /item/\\d+=/item/{id} ", "  /msg/\\d+=/msg/{other} "))))
                .when(httpServerConfig).matchPatterns();
        HttpClientConfig httpClientConfig = Mockito.mock(HttpClientConfig.class);
        VertxConfig vertxConfig = Mockito.mock(VertxConfig.class);
        HttpBinderConfiguration binderConfig = new HttpBinderConfiguration(
                true, false,
                httpServerConfig, httpClientConfig, vertxConfig);

        Assertions.assertFalse(binderConfig.serverMatchPatterns.isEmpty());
        Iterator<Map.Entry<Pattern, String>> i = binderConfig.serverMatchPatterns.entrySet().iterator();
        Map.Entry<Pattern, String> entry = i.next();

        Assertions.assertEquals("/item/\\d+", entry.getKey().pattern());
        Assertions.assertEquals("/item/{id}", entry.getValue());
        Assertions.assertTrue(entry.getKey().matcher("/item/123").matches());

        entry = i.next();
        Assertions.assertEquals("/msg/\\d+", entry.getKey().pattern());
        Assertions.assertEquals("/msg/{other}", entry.getValue());
        Assertions.assertTrue(entry.getKey().matcher("/msg/789").matches());
    }
}
