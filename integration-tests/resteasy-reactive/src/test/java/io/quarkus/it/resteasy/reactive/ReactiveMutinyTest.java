package io.quarkus.it.resteasy.reactive;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.is;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

@QuarkusTest
public class ReactiveMutinyTest {

    @Test
    public void testHello() {
        get("/reactive/hello")
                .then()
                .body(is("hello"))
                .statusCode(200);
    }

    @Test
    public void testFail() {
        get("/reactive/fail")
                .then()
                .statusCode(500);
    }

    @Test
    public void testResponse() {
        get("/reactive/response")
                .then()
                .body(is("hello"))
                .statusCode(202);
    }

    @Test
    public void testHelloAsMulti() {
        get("/reactive/hello/stream")
                .then()
                .contentType("application/json")
                .body("[0]", is("he"))
                .body("[1]", is("ll"))
                .body("[2]", is("o"))
                .statusCode(200);
    }

    @Test
    public void testSerialization() {
        get("/reactive/pet")
                .then()
                .contentType("application/json")
                .body("name", is("neo"))
                .body("kind", is("rabbit"))
                .statusCode(200);
    }

    @Test
    public void testMultiWithSerialization() {
        get("/reactive/pet/stream")
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
        WebTarget target = client.target("http://localhost:" + RestAssured.port + "/reactive/pets");
        try (SseEventSource eventSource = SseEventSource.target(target).build()) {
            Uni<List<Pet>> petList = Uni.createFrom().emitter(new Consumer<UniEmitter<? super List<Pet>>>() {
                @Override
                public void accept(UniEmitter<? super List<Pet>> uniEmitter) {
                    List<Pet> pets = new CopyOnWriteArrayList<>();
                    eventSource.register(event -> {
                        Pet pet = event.readData(Pet.class, MediaType.APPLICATION_JSON_TYPE);
                        pets.add(pet);
                        if (pets.size() == 5) {
                            uniEmitter.complete(pets);
                        }
                    }, ex -> {
                        uniEmitter.fail(new IllegalStateException("SSE failure", ex));
                    });
                    eventSource.open();

                }
            });
            List<Pet> pets = petList.await().atMost(Duration.ofMinutes(1));
            Assertions.assertEquals(5, pets.size());
            Assertions.assertEquals("neo", pets.get(0).getName());
            Assertions.assertEquals("indy", pets.get(1).getName());
            Assertions.assertEquals("plume", pets.get(2).getName());
            Assertions.assertEquals("titi", pets.get(3).getName());
            Assertions.assertEquals("rex", pets.get(4).getName());
        }
    }

    @Test
    public void testClientReturningUni() {
        get("/reactive/client")
                .then()
                .body(is("hello"))
                .statusCode(200);
    }

    @Test
    public void testClientReturningUniOfPet() {
        get("/reactive/client/pet")
                .then()
                .contentType("application/json")
                .body("name", is("neo"))
                .body("kind", is("rabbit"))
                .statusCode(200);
    }

}
