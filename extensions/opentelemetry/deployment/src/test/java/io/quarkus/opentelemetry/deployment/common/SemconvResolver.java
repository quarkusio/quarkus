package io.quarkus.opentelemetry.deployment.common;

import static io.opentelemetry.api.common.AttributeType.LONG;
import static io.opentelemetry.api.common.AttributeType.STRING;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;

public class SemconvResolver {

    SemconvResolver() {
        // empty
    }

    public static void assertTarget(final SpanData server, final String path, final String query) {
        assertEquals(path, server.getAttributes().get(URL_PATH));
        assertEquals(query, server.getAttributes().get(URL_QUERY));
    }

    public static <T> void assertSemanticAttribute(final SpanData spanData, final T expected,
            final AttributeKey<T> attributeKey) {
        assertEquals(expected, getAttribute(spanData, attributeKey));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getAttribute(final SpanData data, final AttributeKey<T> attributeKey) {
        if (attributeKey.getType().equals(LONG)) {
            return (T) data.getAttributes().get(attributeKey);
        } else if (attributeKey.getType().equals(STRING)) {
            return (T) data.getAttributes().get(attributeKey);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported attribute: " + attributeKey.getKey() +
                            " with type: " + attributeKey.getKey().getClass());
        }
    }
}
