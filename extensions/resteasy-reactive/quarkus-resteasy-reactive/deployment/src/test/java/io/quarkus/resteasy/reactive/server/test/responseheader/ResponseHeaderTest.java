package io.quarkus.resteasy.reactive.server.test.responseheader;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.common.Header;
import io.quarkus.resteasy.reactive.server.common.ResponseHeader;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

public class ResponseHeaderTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void should_return_added_headers() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Path("/test")
    public static class TestResource {

        @ResponseHeader(headers = {
                @Header(name = "Access-Control-Allow-Origin", value = "*"),
                @Header(name = "Keep-Alive", value = "timeout=5, max=997"),
        })
        @GET
        public Uni<String> getTest() {
            return Uni.createFrom().item("test");
        }
    }
}
