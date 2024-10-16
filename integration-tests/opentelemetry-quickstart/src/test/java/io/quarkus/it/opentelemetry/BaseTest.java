package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;

import java.util.List;
import java.util.Map;

import io.restassured.common.mapper.TypeRef;

public class BaseTest {

    protected List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    protected void buildGlobalTelemetryInstance() {
        // Do nothing in JVM mode
    }
}
