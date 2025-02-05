package io.quarkus.rest.client.reactive;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ReceiveInputStreamTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.rest-client.test.url", "http://localhost:${quarkus.http.test-port:8081}");

    @Test
    public void test() {
        when()
                .get("test/string")
                .then()
                .statusCode(200)
                .body(is("origin"));

        when()
                .get("test/inputStream")
                .then()
                .statusCode(200)
                .body(is("origin"));
    }

    @Path("test")
    @RegisterRestClient(configKey = "test")
    public interface Client {

        @GET
        @Path("origin")
        InputStream origin();
    }

    @Path("test")
    public static class Resource {

        private final Client client;

        public Resource(@RestClient Client client) {
            this.client = client;
        }

        @GET
        @Path("string")
        public String string() throws IOException {
            try (InputStream extensionsById = client.origin()) {
                return new String(extensionsById.readAllBytes());
            }
        }

        @GET
        @Path("inputStream")
        public InputStream inputStream() {
            return client.origin();
        }

        @GET
        @Path("origin")
        public String origin() {
            return "origin";
        }
    }
}
