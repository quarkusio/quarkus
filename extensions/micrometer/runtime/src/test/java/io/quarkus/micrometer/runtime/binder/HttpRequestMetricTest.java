package io.quarkus.micrometer.runtime.binder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Disabled on Java 8 because of Mocks
 */
@DisabledOnJre(JRE.JAVA_8)
public class HttpRequestMetricTest {

    final List<Pattern> NO_IGNORE_PATTERNS = Collections.emptyList();
    final List<Pattern> ignorePatterns = Arrays.asList(Pattern.compile("/ignore.*"));

    final Map<Pattern, String> NO_MATCH_PATTERNS = Collections.emptyMap();

    @Test
    public void testReturnPathFromHttpRequestPath() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);

        Mockito.when(requestMetric.routingContext.get(HttpRequestMetric.HTTP_REQUEST_PATH))
                .thenReturn("/item/{id}");

        Assertions.assertEquals("/item/{id}", requestMetric.getHttpRequestPath());
    }

    @Test
    public void testReturnPathFromRoutingContext() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);
        Route currentRoute = Mockito.mock(Route.class);

        Mockito.when(requestMetric.routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(currentRoute.getPath()).thenReturn("/item");

        Assertions.assertEquals("/item", requestMetric.getHttpRequestPath());
    }

    @Test
    public void testReturnGenericPathFromRoutingContext() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "/");
        requestMetric.routingContext = Mockito.mock(RoutingContext.class);
        Route currentRoute = Mockito.mock(Route.class);

        Mockito.when(requestMetric.routingContext.currentRoute()).thenReturn(currentRoute);
        Mockito.when(currentRoute.getPath()).thenReturn("/item/:id");

        Assertions.assertEquals("/item/{id}", requestMetric.getHttpRequestPath());
        // Make sure conversion is cached
        Assertions.assertEquals("/item/{id}", HttpRequestMetric.templatePath.get("/item/:id"));
    }

    @Test
    public void testParsePathDoubleSlash() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "//");
        Assertions.assertEquals("/", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParseEmptyPath() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS, "");
        Assertions.assertEquals("/", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathNoLeadingSlash() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "path/with/no/leading/slash");
        Assertions.assertEquals("/path/with/no/leading/slash", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathWithQueryString() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, NO_IGNORE_PATTERNS,
                "/path/with/query/string?stuff");
        Assertions.assertEquals("/path/with/query/string", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathIgnoreNoLeadingSlash() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, ignorePatterns,
                "ignore/me/with/no/leading/slash");
        Assertions.assertEquals("/ignore/me/with/no/leading/slash", requestMetric.path);
        Assertions.assertFalse(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathIgnoreWithQueryString() {
        HttpRequestMetric requestMetric = new HttpRequestMetric(NO_MATCH_PATTERNS, ignorePatterns,
                "/ignore/me/with/query/string?stuff");
        Assertions.assertEquals("/ignore/me/with/query/string", requestMetric.path);
        Assertions.assertFalse(requestMetric.measure, "Path should be measured");
        Assertions.assertFalse(requestMetric.pathMatched, "Path should not be marked as matched");
    }

    @Test
    public void testParsePathMatchReplaceNoLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        HttpRequestMetric requestMetric = new HttpRequestMetric(matchPatterns, NO_IGNORE_PATTERNS, "item/123");
        Assertions.assertEquals("/item/{id}", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertTrue(requestMetric.pathMatched, "Path should be marked as matched");
    }

    @Test
    public void testParsePathMatchReplaceLeadingSlash() {
        final Map<Pattern, String> matchPatterns = new HashMap<>();
        matchPatterns.put(Pattern.compile("/item/\\d+"), "/item/{id}");

        HttpRequestMetric requestMetric = new HttpRequestMetric(matchPatterns, NO_IGNORE_PATTERNS, "/item/123");
        Assertions.assertEquals("/item/{id}", requestMetric.path);
        Assertions.assertTrue(requestMetric.measure, "Path should be measured");
        Assertions.assertTrue(requestMetric.pathMatched, "Path should be marked as matched");
    }
}
