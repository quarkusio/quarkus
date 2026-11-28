package io.quarkus.it.opentelemetry.vertx;

import static io.restassured.RestAssured.given;

import java.util.List;

import io.restassured.common.mapper.TypeRef;

public abstract class SpanExporterBaseTest {

    static List<String> getMessages() {
        return given().get("/bus/messages").body().as(new TypeRef<>() {
        });
    }
}
