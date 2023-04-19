package io.quarkus.opentelemetry.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.quarkus.vertx.core.runtime.VertxMDC;

public final class OpenTelemetryUtil {
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String SAMPLED = "sampled";
    public static final String PARENT_ID = "parentId";

    private OpenTelemetryUtil() {
    }

    /**
     * Converts a list of "key=value" pairs into a map.
     * Empty entries will be removed.
     * In case of duplicate keys, the latest takes precedence.
     *
     * @param headers nullable list of "key=value" pairs
     * @return non-null map of key-value pairs
     */
    public static Map<String, String> convertKeyValueListToMap(List<String> headers) {
        if (headers == null) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (String header : headers) {
            if (header.isEmpty()) {
                continue;
            }
            String[] parts = header.split("=", 2);
            String key = parts[0].trim();
            String value = parts[1].trim();
            result.put(key, value);
        }

        return result;
    }

    /**
     * Sets MDC data by using the current span from the context.
     *
     * @param context opentelemetry context
     * @param vertxContext vertx context
     */
    public static void setMDCData(Context context, io.vertx.core.Context vertxContext) {
        Span span = Span.fromContextOrNull(context);
        if (span != null) {
            SpanContext spanContext = span.getSpanContext();
            VertxMDC vertxMDC = VertxMDC.INSTANCE;
            vertxMDC.put(SPAN_ID, spanContext.getSpanId(), vertxContext);
            vertxMDC.put(TRACE_ID, spanContext.getTraceId(), vertxContext);
            vertxMDC.put(SAMPLED, Boolean.toString(spanContext.isSampled()), vertxContext);
            if (span instanceof ReadableSpan) {
                SpanContext parentSpanContext = ((ReadableSpan) span).getParentSpanContext();
                if (parentSpanContext.isValid()) {
                    vertxMDC.put(PARENT_ID, parentSpanContext.getSpanId(), vertxContext);
                } else {
                    vertxMDC.remove(PARENT_ID, vertxContext);
                }
            }
        }
    }

    /**
     * Clears MDC data related to OpenTelemetry
     *
     * @param vertxContext vertx context
     */
    public static void clearMDCData(io.vertx.core.Context vertxContext) {
        VertxMDC vertxMDC = VertxMDC.INSTANCE;
        vertxMDC.remove(TRACE_ID, vertxContext);
        vertxMDC.remove(SPAN_ID, vertxContext);
        vertxMDC.remove(PARENT_ID, vertxContext);
        vertxMDC.remove(SAMPLED, vertxContext);
    }
}
