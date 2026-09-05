package io.quarkus.it.envers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class OutputResourceTest {
    private static final String RESOURCE_PATH = "/jpa-envers-test/output";

    @TestHTTPResource
    URL url;

    @Test
    void test() {
        given().accept(ContentType.JSON)
                .when()
                .get(RESOURCE_PATH + "2")
                .then()
                .statusCode(200)
                .body("data", equalTo("out"));
    }

    @Test
    public void testSseWithAnnotation() throws InterruptedException, URISyntaxException, MalformedURLException {
        System.out.println(url);
        doTestSee("annotation", "dummy");
    }

    @Test
    public void testSseWithoutAnnotation() throws InterruptedException, URISyntaxException, MalformedURLException {
        doTestSee("no-annotation", "out");
    }

    private void doTestSee(String path, String expectedDataValue)
            throws URISyntaxException, InterruptedException, MalformedURLException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(url.toURI()).path(RESOURCE_PATH).path(path);
        try (SseEventSource sse = SseEventSource.target(target).build()) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            List<String> results = new CopyOnWriteArrayList<>();
            sse.register(event -> results.add(event.readData()), errors::add, latch::countDown);
            sse.open();
            Assertions.assertTrue(latch.await(20, TimeUnit.SECONDS));

            String json = String.format("{\"data\": \"%s\"}", expectedDataValue);
            Assertions.assertEquals(Arrays.asList(json, json), results);
            Assertions.assertEquals(0, errors.size());
        }
    }
}
