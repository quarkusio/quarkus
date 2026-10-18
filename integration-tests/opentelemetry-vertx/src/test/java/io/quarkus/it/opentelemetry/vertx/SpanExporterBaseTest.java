package io.quarkus.it.opentelemetry.vertx;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.opentelemetry.api.trace.SpanKind;
import io.restassured.common.mapper.TypeRef;

public abstract class SpanExporterBaseTest {

    String printSpans(List<Map<String, Object>> spans) {
        if (spans.isEmpty()) {
            return "empty";
        }
        return spans.stream()
                .map(stringObjectMap -> stringObjectMap.get("spanId") + " -> " + stringObjectMap.get("parentSpanId") + " - " +
                        stringObjectMap.get("kind") + " - " +
                        stringObjectMap.get("http.route") + "\n")
                .collect(Collectors.joining());
    }

    Boolean spanSize(int expected) {
        List<Map<String, Object>> spans = getSpans();
        int size = spans.size();
        if (size == expected) {
            return true;
        } else {
            System.out.println("Reset but span remain: " + printSpans(spans));
            return false;
        }
    }

    static List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    static List<String> getMessages() {
        return given().get("/bus/messages").body().as(new TypeRef<>() {
        });
    }

    static List<Map<String, Object>> getSpansByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        return spans.stream()
                .filter(map -> map.get("kind").equals(kind.toString()))
                .filter(map -> map.get("parentSpanId").equals(parentSpanId)).collect(toList());
    }

    static Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        List<Map<String, Object>> span = getSpansByKindAndParentId(spans, kind, parentSpanId);
        assertEquals(1, span.size());
        return span.get(0);
    }

    static String getSpanId(Map<String, Object> span) {
        return (String) span.get("spanId");
    }
}
