package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.arc.Arc;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KafkaConnectorTest {

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };
    protected static final TypeRef<List<Fruit>> FRUIT_TYPE_REF = new TypeRef<List<Fruit>>() {
    };

    protected static final TypeRef<ProcessingState<KafkaReceivers.PeopleState>> PEOPLE_STATE_REF = new TypeRef<ProcessingState<KafkaReceivers.PeopleState>>() {
    };

    @Test
    @Order(1)
    public void testPeople() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/people").as(TYPE_REF).size(), 6));
        await().untilAsserted(() -> {
            Response response = get("/kafka/people-state/{key}", "people-checkpoint:people:0");
            Assertions.assertNotNull(response);
            Assertions.assertTrue(response.asString().length() > 0);
            ProcessingState<KafkaReceivers.PeopleState> state = response.as(PEOPLE_STATE_REF);
            Assertions.assertNotNull(state);
            Assertions.assertEquals("bob;alice;tom;jerry;anna;ken", state.getState().names);
        });
    }

    @Test
    @Order(2)
    public void testPets() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/pets").as(TYPE_REF).size(), 3));
    }

    @Disabled("MultiSplitter yields flaky results, to investigate")
    @Test
    @Order(3)
    public void testFruits() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/fruits").as(FRUIT_TYPE_REF).size(), 5));
    }

    @Test
    @DisabledOnIntegrationTest
    @Order(4)
    public void testPrices() {
        KafkaRepeatableReceivers repeatableReceivers = Arc.container().instance(KafkaRepeatableReceivers.class).get();
        await().untilAsserted(() -> Assertions.assertEquals(repeatableReceivers.getPrices().size(), 6));
    }

    @Test
    @Order(5)
    public void testDataWithMetadata() {
        await().untilAsserted(() -> {
            Map<String, String> map = get("/kafka/data-with-metadata").as(new TypeRef<Map<String, String>>() {
            });
            Assertions.assertEquals(3, map.size());
            Assertions.assertEquals("a", map.get("a"));
            Assertions.assertEquals("a", map.get("a"));
            Assertions.assertEquals("b", map.get("b"));
        });
    }

    @Test
    @Order(6)
    public void testDataForKeyed() {
        await().untilAsserted(() -> {
            List<String> list = get("/kafka/data-for-keyed").as(new TypeRef<List<String>>() {
            });
            Assertions.assertEquals(3, list.size());
            Assertions.assertTrue(list.contains("A-a"));
            Assertions.assertTrue(list.contains("A-c"));
            Assertions.assertTrue(list.contains("B-b"));
        });
    }

    @Test
    @Order(7)
    public void testRequestReply() {
        List<String> replies = new ArrayList<>();
        replies.add(given().contentType("application/json").body("1").post("/kafka/req-rep").asString());
        replies.add(given().contentType("application/json").body("2").post("/kafka/req-rep").asString());
        replies.add(given().contentType("application/json").body("3").post("/kafka/req-rep").asString());
        replies.add(given().contentType("application/json").body("4").post("/kafka/req-rep").asString());
        replies.add(given().contentType("application/json").body("5").post("/kafka/req-rep").asString());
        Assertions.assertIterableEquals(List.of("reply-1", "reply-2", "reply-3", "reply-4", "reply-5"), replies);
    }

    @Test
    @Order(8)
    void testPrometheusScrapeEndpointOpenMetrics() {
        given().header("Accept", "text/plain; version=0.0.4; charset=utf-8")
                .when().get("/q/metrics")
                .then().statusCode(200)
                .body(containsString("quarkus_messaging_message_duration_seconds_max"))
                .body(containsString("quarkus_messaging_message_duration_seconds_sum"))
                .body(containsString("quarkus_messaging_message_duration_seconds_count"))
                .body(containsString("quarkus_messaging_message_count_total"))
                .body(containsString("quarkus_messaging_message_acks_total"))
                .body(containsString("kafka_app_info_start_time_ms"))
                .body(not(containsString("kafka_version=\"unknown\"")));
    }

}
