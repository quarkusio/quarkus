package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
public class KafkaConnectorTest {

    protected static final TypeRef<List<Person>> TYPE_REF = new TypeRef<List<Person>>() {
    };

    protected static final TypeRef<ProcessingState<KafkaReceivers.PeopleState>> PEOPLE_STATE_REF = new TypeRef<ProcessingState<KafkaReceivers.PeopleState>>() {
    };

    @Test
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
    public void testPets() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/pets").as(TYPE_REF).size(), 3));
    }

    @Test
    public void testFruits() {
        await().untilAsserted(() -> Assertions.assertEquals(get("/kafka/fruits").as(TYPE_REF).size(), 4));
    }

    @Test
    @DisabledOnIntegrationTest
    public void testPrices() {
        KafkaRepeatableReceivers repeatableReceivers = Arc.container().instance(KafkaRepeatableReceivers.class).get();
        await().untilAsserted(() -> Assertions.assertEquals(repeatableReceivers.getPrices().size(), 6));
    }

    @Test
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

}
