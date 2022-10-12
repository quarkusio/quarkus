package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaConnectorTest {

    protected static final TypeRef<List<Fruit>> FRUIT_TYPE_REF = new TypeRef<List<Fruit>>() {
    };

    protected static final TypeRef<List<Person>> PERSON_TYPE_REF = new TypeRef<List<Person>>() {
    };

    protected static final TypeRef<PeopleState> PEOPLE_STATE_TYPE_REF = new TypeRef<PeopleState>() {
    };

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

        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/fruits").as(FRUIT_TYPE_REF).size()));
    }

    @Test
    public void testPeople() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/people").as(PERSON_TYPE_REF).size(), 6));
        await().untilAsserted(() -> {
            io.restassured.response.Response response = get("/kafka/people-state/{consumerGroupId}/{topic}/{partition}",
                    "people-checkpoint", "people", 0);
            Assertions.assertNotNull(response);
            Assertions.assertTrue(response.asString().length() > 0);
            PeopleState state = response.as(PEOPLE_STATE_TYPE_REF);
            Assertions.assertNotNull(state);
            Assertions.assertEquals("bob;alice;tom;jerry;anna;ken", state.names);
        });
    }

}
