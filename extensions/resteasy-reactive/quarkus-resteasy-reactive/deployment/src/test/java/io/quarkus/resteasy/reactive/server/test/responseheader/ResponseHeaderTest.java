package io.quarkus.resteasy.reactive.server.test.responseheader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.Header;
import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ResponseHeaderTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void should_return_added_headers_uni() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test/multi")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void should_return_added_headers_multi() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test/multi")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void should_throw_exception_without_headers_uni() {
        Headers headers = RestAssured.given().get("/test/exception_uni")
                .then().extract().headers();
        assertFalse(headers.hasHeaderWithName("Access-Control-Allow-Origin"));

    }

    @Test
    public void should_throw_exception_without_headers_multi() {
        Headers headers = RestAssured.given().get("/test/exception_multi")
                .then().extract().headers();
        assertFalse(headers.hasHeaderWithName("Access-Control-Allow-Origin"));
    }

    @Path("/test")
    public static class TestResource {

        @ResponseHeader(headers = {
                @Header(name = "Access-Control-Allow-Origin", value = "*"),
                @Header(name = "Keep-Alive", value = "timeout=5, max=997"),
        })
        @GET
        @Path(("/uni"))
        public Uni<String> getTestUni() {
            return Uni.createFrom().item("test");
        }

        @ResponseHeader(headers = {
                @Header(name = "Access-Control-Allow-Origin", value = "*"),
                @Header(name = "Keep-Alive", value = "timeout=5, max=997"),
        })
        @GET
        @Path("/multi")
        public Multi<String> getTestMulti() {
            return Multi.createFrom().item("test");
        }

        @ResponseHeader(headers = {
                @Header(name = "Access-Control-Allow-Origin", value = "*")
        })
        @GET
        @Path(("/exception_uni"))
        public Uni<String> throwExceptionUni() {
            return Uni.createFrom().failure(new IllegalArgumentException());
        }

        @ResponseHeader(headers = {
                @Header(name = "Access-Control-Allow-Origin", value = "*")
        })
        @GET
        @Path("/exception_multi")
        public Multi<String> throwExceptionMulti() {
            return Multi.createFrom().failure(new IllegalArgumentException());
        }
    }
}
