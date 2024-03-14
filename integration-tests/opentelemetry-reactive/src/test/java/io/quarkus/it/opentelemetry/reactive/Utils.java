package io.quarkus.it.opentelemetry.reactive;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import io.opentelemetry.api.trace.SpanKind;
import io.restassured.common.mapper.TypeRef;

public final class Utils {

    private Utils() {

    }

    public static List<Map<String, Object>> getSpans() {
        return when().get("/export").body().as(new TypeRef<>() {
        });
    }

    public static Map<String, Object> getSpanEventAttrs(String spanName, String eventName) {
        return given()
                .queryParam("spanName", spanName)
                .queryParam("eventName", eventName)
                .get("/export-event-attributes").body().as(new TypeRef<>() {
                });
    }

    public static List<String> getExceptionEventData() {
        return when().get("/exportExceptionMessages").body().as(new TypeRef<>() {
        });
    }

    public static Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        List<Map<String, Object>> span = getSpansByKindAndParentId(spans, kind, parentSpanId);
        assertEquals(1, span.size());
        return span.get(0);
    }

    public static List<Map<String, Object>> getSpansByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        return spans.stream()
                .filter(map -> map.get("kind").equals(kind.toString()))
                .filter(map -> map.get("parentSpanId").equals(parentSpanId)).collect(toList());
    }
}
