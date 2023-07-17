package io.quarkus.it.rest.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RequestEncodingTest {

    private static final String ENCODED_PATH = "/with%20space";
    private static final String DECODED_PATH = "/with space";

    @TestHTTPResource("/")
    String root;

    @Test
    void testEncodedPath() throws Exception {
        var request = ClientBuilder.newClient().target(root + ENCODED_PATH).request();
        try (var response = request.get()) {
            assertThat(response.getStatus())
                    .as("Unexpected HTTP status with message %s", response.readEntity(String.class))
                    .isEqualTo(200);
        }
    }

    @Test
    void testDecodedPath() throws Exception {
        var request = ClientBuilder.newClient().target(root + DECODED_PATH).request();
        try (var response = request.get()) {
            assertThat(response.getStatus())
                    .as("Unexpected HTTP status with message %s", response.readEntity(String.class))
                    .isEqualTo(200);
        }
    }

}
