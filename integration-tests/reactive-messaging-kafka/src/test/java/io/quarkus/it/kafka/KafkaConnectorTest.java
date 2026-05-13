package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.HashSet;
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
// this test class needs to be executed first, since it relies on number of kafka messages send
// these messages are send during each test class execution and cannot be easily removed
@Order(1)
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
        await().untilAsserted(() -> Assertions.assertEquals(6, new HashSet<>(repeatableReceivers.getPrices()).size()));
    }

    @Test
    @Order(5)
    public void testDataWithMetadata() {
        await().untilAsserted(() -> {
            Map<String, String> map = get("/kafka/data-with-metadata").as(new TypeRef<Map<String, String>>() {
            });
            Assertions.assertEquals(3, map.size());
            Assertions.assertEquals("a", map.get("a"));
            Assertions.assertEquals("a", map.get("c"));
            Assertions.assertEquals("b", map.get("b"));
        });
    }

    @Test
    @Order(6)
    public void testDataForKeyed() {
        await().untilAsserted(() -> {
            List<String> list = get("/kafka/data-for-keyed").as(new TypeRef<List<String>>() {
            });
            Assertions.assertEquals(3, new HashSet<>(list).size());
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
    public void testExactlyOnceProcessing() {
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Integer> processed = get("/kafka/exactly-once-processed").as(new TypeRef<List<Integer>>() {
            });
            Assertions.assertTrue(processed.size() >= 6, "Expected at least 6 processed, got " + processed.size());
        });
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Integer> results = get("/kafka/exactly-once-results").as(new TypeRef<List<Integer>>() {
            });
            Assertions.assertTrue(results.size() >= 6, "Expected at least 6 results, got " + results.size());
            // values should be original + 1
            Assertions.assertTrue(results.contains(1));
            Assertions.assertTrue(results.contains(2));
            Assertions.assertTrue(results.contains(3));
        });
    }

    @Test
    @Order(9)
    public void testExactlyOnceListProcessing() {
        // Each input record produces 2 output records (value*10 and value*10+1)
        // 4 input records -> 8 output records
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Integer> processed = get("/kafka/exactly-once-list-processed").as(new TypeRef<List<Integer>>() {
            });
            Assertions.assertTrue(processed.size() >= 4, "Expected at least 4 processed, got " + processed.size());
        });
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Integer> results = get("/kafka/exactly-once-list-results").as(new TypeRef<List<Integer>>() {
            });
            Assertions.assertTrue(results.size() >= 8, "Expected at least 8 results, got " + results.size());
            // input 0 -> 0, 1; input 1 -> 10, 11; input 2 -> 20, 21; input 3 -> 30, 31
            Assertions.assertTrue(results.contains(0));
            Assertions.assertTrue(results.contains(1));
            Assertions.assertTrue(results.contains(10));
            Assertions.assertTrue(results.contains(11));
            Assertions.assertTrue(results.contains(20));
            Assertions.assertTrue(results.contains(21));
        });
    }

    @Test
    @Order(10)
    public void testExactlyOnceUniProcessing() {
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Integer> processed = get("/kafka/exactly-once-uni-processed").as(new TypeRef<List<Integer>>() {
            });
            Assertions.assertTrue(processed.size() >= 6, "Expected at least 6 processed, got " + processed.size());
        });
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Integer> results = get("/kafka/exactly-once-uni-results").as(new TypeRef<List<Integer>>() {
            });
            Assertions.assertTrue(results.size() >= 6, "Expected at least 6 results, got " + results.size());
            // values should be original + 1
            Assertions.assertTrue(results.contains(1));
            Assertions.assertTrue(results.contains(2));
            Assertions.assertTrue(results.contains(3));
        });
    }

    @Test
    @Order(11)
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
