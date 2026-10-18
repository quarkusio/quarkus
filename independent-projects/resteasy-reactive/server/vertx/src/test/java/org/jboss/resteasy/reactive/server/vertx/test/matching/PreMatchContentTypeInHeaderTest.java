package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PreMatchContentTypeInHeaderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class);
                }
            });

    @Test
    public void filterNotSettingContentType() {
        given()
                .header("Content-Type", "application/json")
                .body("[]")
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(is("json-[]"));

        given()
                .header("Content-Type", "text/plain")
                .body("input")
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(is("text-input"));
    }

    @Test
    public void filterSettingContentTypeToText() {
        given()
                .header("Content-Type", "application/json")
                .header("test-content-type", "text/plain")
                .body("[]")
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(is("text-[]"));
    }

    @Test
    public void filterSettingContentTypeToJson() {
        given()
                .header("Content-Type", "text/plain")
                .header("test-content-type", "application/json")
                .body("input")
                .when()
                .post("/test")
                .then()
                .statusCode(200)
                .body(is("json-input"));
    }

    @Path("test")
    public static class TestResource {

        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        public String fromText(String input) {
            return "text-" + input;
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public String fromJson(String input) {
            return "json-" + input;
        }
    }

    public record Result(String message) {

    }

    @Provider
    @PreMatching
    public static class Filter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            String testContentType = context.getHeaderString("test-content-type");
            if (testContentType != null) {
                context.getHeaders().putSingle("Content-Type", testContentType);
            }
        }
    }
}
