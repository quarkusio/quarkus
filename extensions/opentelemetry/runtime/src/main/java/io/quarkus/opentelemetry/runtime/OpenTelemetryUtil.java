package io.quarkus.opentelemetry.runtime;

import java.util.Collections;
import java.util.HashMap;
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
     * <p>
     * This method is in the hot path and was optimized to not use getSpanData()
     *
     * @param context opentelemetry context
     * @param vertxContext vertx context
     */
    public static void setMDCData(Context context, io.vertx.core.Context vertxContext) {
        if (context == null) {
            return;
        }

        Span span = Span.fromContextOrNull(context);
        if (span != null) {
            SpanContext spanContext = span.getSpanContext();
            VertxMDC.INSTANCE.put(SPAN_ID, spanContext.getSpanId(), vertxContext);
            VertxMDC.INSTANCE.put(TRACE_ID, spanContext.getTraceId(), vertxContext);
            VertxMDC.INSTANCE.put(SAMPLED, Boolean.toString(spanContext.isSampled()), vertxContext);
            if (span instanceof ReadableSpan) {
                SpanContext parentSpanContext = ((ReadableSpan) span).getParentSpanContext();
                if (parentSpanContext != null && parentSpanContext.isValid()) {
                    VertxMDC.INSTANCE.put(PARENT_ID, parentSpanContext.getSpanId(), vertxContext);
                }
            }
        }
    }

    /**
     * Gets current span data from the MDC context.
     *
     * @param context opentelemetry context
     */
    public static Map<String, String> getSpanData(Context context) {
        if (context == null) {
            return Collections.emptyMap();
        }
        Span span = Span.fromContextOrNull(context);
        Map<String, String> spanData = new HashMap<>(4, 1f);
        if (span != null) {
            SpanContext spanContext = span.getSpanContext();
            spanData.put(SPAN_ID, spanContext.getSpanId());
            spanData.put(TRACE_ID, spanContext.getTraceId());
            spanData.put(SAMPLED, Boolean.toString(spanContext.isSampled()));
            if (span instanceof ReadableSpan) {
                SpanContext parentSpanContext = ((ReadableSpan) span).getParentSpanContext();
                if (parentSpanContext != null && parentSpanContext.isValid()) {
                    spanData.put(PARENT_ID, parentSpanContext.getSpanId());
                }
            }
        }
        return spanData;
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
