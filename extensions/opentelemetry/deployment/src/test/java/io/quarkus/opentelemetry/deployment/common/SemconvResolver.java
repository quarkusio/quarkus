package io.quarkus.opentelemetry.deployment.common;

import static io.opentelemetry.api.common.AttributeType.LONG;
import static io.opentelemetry.api.common.AttributeType.STRING;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.SemanticAttributes.URL_PATH;
import static io.opentelemetry.semconv.SemanticAttributes.URL_QUERY;
import static io.quarkus.opentelemetry.runtime.config.runtime.SemconvStabilityType.HTTP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.config.runtime.SemconvStabilityType;

public class SemconvResolver {

    public static final SemconvStabilityType SEMCONV_STABILITY_TYPE;
    private static final Map<String, String> conventionsMapper = new HashMap<>();
    private static final Logger log = Logger.getLogger(SemconvResolver.class);

    static {
        SEMCONV_STABILITY_TYPE = OpenTelemetryUtil.getSemconvStabilityOptin(
                System.getProperty("quarkus.otel.semconv-stability.opt-in", "stable"));

        log.info("Using semantic convention stability type: " + SEMCONV_STABILITY_TYPE);

        conventionsMapper.put("http.method", "http.request.method");
        conventionsMapper.put("http.status_code", "http.response.status_code");
        conventionsMapper.put("http.request_content_length", "http.request.body.size");
        conventionsMapper.put("http.response_content_length", "http.request.body.size");
        conventionsMapper.put("net.protocol.name", "network.protocol.name");
        conventionsMapper.put("net.protocol.version", "network.protocol.version");
        // net.sock.family removed
        conventionsMapper.put("net.sock.peer.addr", "network.peer.address");
        conventionsMapper.put("net.sock.peer.port", "network.peer.port");
        // net.sock.peer.name	removed
        // New: http.request.method_original
        // New: error.type
        conventionsMapper.put("http.url", "url.full");
        conventionsMapper.put("http.resend_count", "http.request.resend_count");
        conventionsMapper.put("net.peer.name", "server.address");
        conventionsMapper.put("net.peer.port", "server.port");
        // http.target split into url.path and url.query
        conventionsMapper.put("http.scheme", "url.scheme");
        conventionsMapper.put("http.client_ip", "client.address");
        conventionsMapper.put("net.host.name", "server.address");
        conventionsMapper.put("net.host.port", "server.port");

    }

    SemconvResolver() {
        // empty
    }

    public static void assertTarget(final SpanData server, final String path, final String query) {
        switch (SEMCONV_STABILITY_TYPE) {
            case HTTP:
                assertEquals(path, server.getAttributes().get(URL_PATH));
                assertEquals(query, server.getAttributes().get(URL_QUERY));
                break;
            case HTTP_DUP:
                assertEquals(path, server.getAttributes().get(URL_PATH));
                assertEquals(query, server.getAttributes().get(URL_QUERY));
                assertEquals("" + path + (query == null ? "" : "?" + query), server.getAttributes().get(HTTP_TARGET));
                break;
            case HTTP_OLD:
                assertEquals("" + path + (query == null ? "" : "?" + query), server.getAttributes().get(HTTP_TARGET));
                break;
            default:
                throw new IllegalArgumentException("Unsupported semantic convention stability type: " + SEMCONV_STABILITY_TYPE);
        }
    }

    public static <T> void assertSemanticAttribute(final SpanData spanData, final T expected,
            final AttributeKey<T> attribute) {
        switch (SEMCONV_STABILITY_TYPE) {
            case HTTP:
                assertEquals(expected, getNewAttribute(spanData, attribute));
                break;
            case HTTP_DUP:
                assertEquals(expected, getNewAttribute(spanData, attribute)); // assert new semantic convention
                assertEquals(expected, spanData.getAttributes().get(attribute)); // assert old semantic convention
                break;
            case HTTP_OLD:
                assertEquals(expected, spanData.getAttributes().get(attribute)); // assert old semantic convention
                break;
            default:
                throw new IllegalArgumentException("Unsupported semantic convention stability type: " + SEMCONV_STABILITY_TYPE);
        }
    }

    public static <T> void assertNotNullSemanticAttribute(final SpanData spanData,
            final AttributeKey<T> attribute) {
        switch (SemconvResolver.SEMCONV_STABILITY_TYPE) {
            case HTTP:
                assertNotNull(getNewAttribute(spanData, attribute));
                break;
            case HTTP_DUP:
                assertNotNull(getNewAttribute(spanData, attribute)); // assert new semantic convention
                assertNotNull(spanData.getAttributes().get(attribute)); // assert old semantic convention
                break;
            case HTTP_OLD:
                assertNotNull(spanData.getAttributes().get(attribute)); // assert old semantic convention
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported semantic convention stability type: " + SemconvResolver.SEMCONV_STABILITY_TYPE);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getNewAttribute(final SpanData data, final AttributeKey<T> legacyAttributeKey) {
        if (legacyAttributeKey.getType().equals(LONG)) {
            return (T) data.getAttributes().get(resolveLong((AttributeKey<Long>) legacyAttributeKey));
        } else if (legacyAttributeKey.getType().equals(STRING)) {
            return (T) data.getAttributes().get(resolveString((AttributeKey<String>) legacyAttributeKey));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported attribute: " + legacyAttributeKey.getKey() +
                            " with type: " + legacyAttributeKey.getKey().getClass());
        }
    }

    private static AttributeKey<String> resolveString(final AttributeKey<String> legacyKey) {
        return AttributeKey.stringKey(conventionsMapper.get(legacyKey.getKey()));
    }

    private static AttributeKey<Long> resolveLong(final AttributeKey<Long> legacyKey) {
        return AttributeKey.longKey(conventionsMapper.get(legacyKey.getKey()));
    }
}
