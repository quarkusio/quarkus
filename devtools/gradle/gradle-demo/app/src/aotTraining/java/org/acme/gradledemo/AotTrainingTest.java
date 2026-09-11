package org.acme.gradledemo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class AotTrainingTest {

    @Test
    void applicationStartsForTraining() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8081/dogs")).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }
}
