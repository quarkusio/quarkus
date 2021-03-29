package io.quarkus.micrometer.runtime.binder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestMetricInfoTest {

    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final List<Pattern> ignorePatterns = Arrays.asList(Pattern.compile("/ignore.*"));

    final Map<Pattern, String> NO_MATCH_PATTERNS = Collections.emptyMap();

    @Test
    public void testParsePathDoubleSlash() {
        RequestMetricInfo requestMetric = new RequestMetricInfo(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "//");
        Assertions.assertEquals("/", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParseEmptyPath() {
        RequestMetricInfo requestMetric = new RequestMetricInfo(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "");
        Assertions.assertEquals("/", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathNoLeadingSlash() {
        RequestMetricInfo requestMetric = new RequestMetricInfo(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "path/with/no/leading/slash");
        Assertions.assertEquals("/path/with/no/leading/slash", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathWithQueryString() {
        RequestMetricInfo requestMetric = new RequestMetricInfo(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "/path/with/query/string?stuff");
        Assertions.assertEquals("/path/with/query/string", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathIgnoreNoLeadingSlash() {
        RequestMetricInfo requestMetric = new RequestMetricInfo(NO_MATCH_PATTERNS, ignorePatterns,
                "ignore/me/with/no/leading/slash");
        Assertions.assertEquals("/ignore/me/with/no/leading/slash", requestMetric.path);
        Assertions.assertFalse(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathIgnoreWithQueryString() {
        RequestMetricInfo requestMetric = new RequestMetricInfo(NO_MATCH_PATTERNS, ignorePatterns,
                "/ignore/me/with/query/string?stuff");
        Assertions.assertEquals("/ignore/me/with/query/string", requestMetric.path);
        Assertions.assertFalse(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathMatchReplaceNoLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        RequestMetricInfo requestMetric = new RequestMetricInfo(matchPatterns, NO_IGNORE_PATTERNS, "item/123");
        Assertions.assertEquals("/item/{id}", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertTrue(requestMetric.pathMatched, "Path should be marked as matched");
    }

    @Test
    public void testParsePathMatchReplaceLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        RequestMetricInfo requestMetric = new RequestMetricInfo(matchPatterns, NO_IGNORE_PATTERNS, "/item/123");
        Assertions.assertEquals("/item/{id}", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertTrue(requestMetric.pathMatched, "Path should be marked as matched");
    }
}
