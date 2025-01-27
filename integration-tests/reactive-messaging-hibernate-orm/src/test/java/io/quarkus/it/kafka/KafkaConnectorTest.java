package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.fruit.Fruit;
import io.quarkus.it.kafka.people.PeopleState;
import io.quarkus.it.kafka.people.Person;
import io.quarkus.it.kafka.pet.Pet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaConnectorTest {

    protected static final TypeRef<List<Fruit>> TYPE_REF = new TypeRef<List<Fruit>>() {
    };

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    public void testFruits() {
        given().body(new Fruit("apple")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("banana")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("peach")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("orange")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("cherry")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("pear")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/fruits").as(TYPE_REF).size()));

        for (ConsumerRecord<String, String> record : companion
                .consumeWithDeserializers(new StringDeserializer(), new StringDeserializer())
                .fromTopics("fruits-persisted", 6)
                .awaitCompletion()) {
            System.out.println(record);
        }
    }

    protected static final TypeRef<List<Person>> PERSON_TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void testPeople() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/people").as(PERSON_TYPE_REF).size(), 6));
        await().untilAsserted(() -> {
            PeopleState result = get("/kafka/people-state").as(PeopleState.class);
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.offset >= 6);
            Assertions.assertEquals("bob;alice;tom;jerry;anna;ken", result.getNames());
        });
    }

    protected static final TypeRef<List<Pet>> PET_TYPE_REF = new TypeRef<List<Pet>>() {
    };

    @Test
    public void testPets() {
        await().untilAsserted(() -> Assertions.assertEquals(0, get("/kafka/pets").as(PET_TYPE_REF).size()));

        given().body("cat").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("dog").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("bad").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        given().body("mouse").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("rabbit").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("fish").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("hamster").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/pets").as(PET_TYPE_REF).size()));
        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/pets-consumed").as(PET_TYPE_REF).size()));
    }

}
