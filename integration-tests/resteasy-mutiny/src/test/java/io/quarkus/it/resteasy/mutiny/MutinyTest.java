package io.quarkus.it.resteasy.mutiny;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MutinyTest {

    @Test
    public void testHello() {
        get("/mutiny/hello")
                .then()
                .body(is("hello"))
                .statusCode(200);
    }

    @Test
    public void testFail() {
        get("/mutiny/fail")
                .then()
                .statusCode(500);
    }

    @Test
    public void testResponse() {
        get("/mutiny/response")
                .then()
                .body(is("hello"))
                .statusCode(202);
    }

    @Test
    public void testHelloAsMulti() {
        get("/mutiny/hello/stream")
                .then()
                .contentType("application/json")
                .body("[0]", is("he"))
                .body("[1]", is("ll"))
                .body("[2]", is("o"))
                .statusCode(200);
    }

    @Test
    public void testSerialization() {
        get("/mutiny/pet")
                .then()
                .contentType("application/json")
                .body("name", is("neo"))
                .body("kind", is("rabbit"))
                .statusCode(200);
    }

    @Test
    public void testMultiWithSerialization() {
        get("/mutiny/pet/stream")
                .then()
                .contentType("application/json")
                .body("[0].name", is("neo"))
                .body("[0].kind", is("rabbit"))
                .body("[1].name", is("indy"))
                .body("[1].kind", is("dog"))
                .statusCode(200);
    }

    @Test
    public void testSSE() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:" + RestAssured.port + "/mutiny/pets");
        SseEventSource source = SseEventSource.target(target).build();
        List<Pet> pets = new CopyOnWriteArrayList<>();
        try (SseEventSource eventSource = source) {
            eventSource.register(event -> {
                Pet pet = event.readData(Pet.class, MediaType.APPLICATION_JSON_TYPE);
                pets.add(pet);
            }, ex -> {
                throw new IllegalStateException("SSE failure", ex);
            });
            eventSource.open();
            await().until(() -> pets.size() == 5);
        }
        Assertions.assertEquals("neo", pets.get(0).getName());
        Assertions.assertEquals("indy", pets.get(1).getName());
        Assertions.assertEquals("plume", pets.get(2).getName());
        Assertions.assertEquals("titi", pets.get(3).getName());
        Assertions.assertEquals("rex", pets.get(4).getName());
    }

    @Test
    public void testClientReturningUni() {
        get("/mutiny/client")
                .then()
                .body(is("hello"))
                .statusCode(200);
    }

    @Test
    public void testClientReturningUniOfPet() {
        get("/mutiny/client/pet")
                .then()
                .contentType("application/json")
                .body("name", is("neo"))
                .body("kind", is("rabbit"))
                .statusCode(200);
    }

}
