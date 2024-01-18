package org.jboss.resteasy.reactive.server.vertx.test.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class SseServerTestCase {

    @RegisterExtension
    static final ResteasyReactiveUnitTest config = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SseServerResource.class));

    @Test
    public void shouldCallOnCloseOnServer() throws InterruptedException {
        System.out.println("####### shouldCallOnCloseOnServer");
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(PortProviderUtil.createURI("/sse/subscribe"));
        try (SseEventSource sse = SseEventSource.target(target).build()) {
            CountDownLatch openingLatch = new CountDownLatch(1);
            List<String> results = new CopyOnWriteArrayList<>();
            sse.register(event -> {
                System.out.println("received data: " + event.readData());
                results.add(event.readData());
                openingLatch.countDown();
            });
            sse.open();
            Assertions.assertTrue(openingLatch.await(3, TimeUnit.SECONDS));
            Assertions.assertEquals(1, results.size());
            sse.close();
            System.out.println("called sse.close() from client");
            RestAssured.get("/sse/onclose-callback")
                    .then()
                    .statusCode(200)
                    .body(Matchers.equalTo("true"));
        }
    }

    @Test
    public void shouldNotTryToSendToClosedSink() throws InterruptedException {
        System.out.println("####### shouldNotTryToSendToClosedSink");
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(PortProviderUtil.createURI("/sse/subscribe"));
        try (SseEventSource sse = SseEventSource.target(target).build()) {
            CountDownLatch openingLatch = new CountDownLatch(1);
            List<String> results = new ArrayList<>();
            sse.register(event -> {
                System.out.println("received data: " + event.readData());
                results.add(event.readData());
                openingLatch.countDown();
            });
            sse.open();
            Assertions.assertTrue(openingLatch.await(3, TimeUnit.SECONDS));
            Assertions.assertEquals(1, results.size());
            sse.close();
            RestAssured.get("/sse/onclose-callback")
                    .then()
                    .statusCode(200)
                    .body(Matchers.equalTo("true"));
            RestAssured.post("/sse/broadcast")
                    .then()
                    .statusCode(200);
            RestAssured.get("/sse/onerror-callback")
                    .then()
                    .statusCode(200)
                    .body(Matchers.equalTo("false"));
        }
    }
}
