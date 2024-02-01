package io.quarkus.it.panache.reactive;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class PanacheFunctionalityTest {

    /**
     * Tests that direct use of the entity in the test class does not break transformation
     *
     * see https://github.com/quarkusio/quarkus/issues/1724
     */
    @SuppressWarnings("unused")
    Person p = new Person();

    @Test
    public void testPanacheFunctionality() throws Exception {
        RestAssured.when().get("/test/model-dao").then().body(is("OK"));
        RestAssured.when().get("/test/model").then().body(is("OK"));
        RestAssured.when().get("/test/accessors").then().body(is("OK"));

        RestAssured.when().get("/test/model1").then().body(is("OK"));
        RestAssured.when().get("/test/model2").then().body(is("OK"));
        RestAssured.when().get("/test/projection1").then().body(is("OK"));
        RestAssured.when().get("/test/projection2").then().body(is("OK"));
        RestAssured.when().get("/test/projection-nested").then().body(is("OK"));
        RestAssured.when().get("/test/model3").then().body(is("OK"));
    }

    @Test
    public void testPanacheSerialisation() {
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/test/ignored-properties")
                .then()
                .body(is("{\"id\":666,\"dogs\":[],\"name\":\"Eddie\",\"serialisationTrick\":1,\"status\":\"DECEASED\"}"));
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    public void testPanacheInTest(UniAsserter asserter) {
        asserter.assertEquals(() -> Panache.withSession(() -> Person.count()), 0l);
    }

    @Test
    public void testBug5274() {
        RestAssured.when().get("/test/5274").then().body(is("OK"));
    }

    @Test
    public void testBug5885() {
        RestAssured.when().get("/test/5885").then().body(is("OK"));
    }

    /**
     * _PanacheEntityBase_ has the method _isPersistent_. This method is used by Jackson to serialize the attribute *persistent*
     * in the JSON which is not intended. This test ensures that the attribute *persistent* is not generated when using Jackson.
     *
     * This test does not interact with the Quarkus application itself. It is just using the Jackson ObjectMapper with a
     * PanacheEntity. Thus this test is disabled in native mode. The test code runs the JVM and not native.
     */
    @DisabledOnIntegrationTest
    @Test
    public void jacksonDeserializationIgnoresPersistentAttribute() throws JsonProcessingException {
        // set Up
        Person person = new Person();
        person.name = "max";
        // do
        ObjectMapper objectMapper = new ObjectMapper();
        // make sure the Jaxb module is loaded
        objectMapper.findAndRegisterModules();
        String personAsString = objectMapper.writeValueAsString(person);
        // check
        // hence no 'persistence'-attribute
        assertEquals(
                "{\"id\":null,\"name\":\"max\",\"uniqueName\":null,\"address\":null,\"description\":null,\"status\":null,\"dogs\":[],\"serialisationTrick\":1}",
                personAsString);
    }

    /**
     * This test is disabled in native mode as there is no interaction with the quarkus integration test endpoint.
     */
    @DisabledOnIntegrationTest
    @Test
    public void jsonbDeserializationHasAllFields() throws JsonProcessingException {
        // set Up
        Person person = new Person();
        person.name = "max";
        // do

        Jsonb jsonb = JsonbBuilder.create();
        String json = jsonb.toJson(person);
        assertEquals(
                "{\"dogs\":[],\"name\":\"max\",\"serialisationTrick\":1}",
                json);
    }

    @Test
    public void testCompositeKey() {
        RestAssured.when()
                .get("/test/composite")
                .then()
                .body(is("OK"));
    }

    @Test
    public void testBug7721() {
        RestAssured.when().get("/test/7721").then().body(is("OK"));
    }

    @Test
    public void testBug8254() {
        RestAssured.when().get("/test/8254").then().body(is("OK"));
    }

    @Test
    public void testBug9025() {
        RestAssured.when().get("/test/9025").then().body(is("OK"));
    }

    @Test
    public void testBug9036() {
        RestAssured.when().get("/test/9036").then().body(is("OK"));
    }

    @Test
    public void testSortByNullPrecedence() {
        RestAssured.when().get("/test/testSortByNullPrecedence").then().body(is("OK"));
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    void testTransaction(UniAsserter asserter) {
        asserter.assertNotNull(() -> Panache.withTransaction(() -> Panache.currentTransaction()));
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    void testNoTransaction(UniAsserter asserter) {
        asserter.assertNull(() -> Panache.withSession(() -> Panache.currentTransaction()));
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    public void testBug7102(UniAsserter asserter) {
        asserter.execute(() -> createBug7102()
                .flatMap(person -> {
                    return getBug7102(person.id)
                            .flatMap(person1 -> {
                                Assertions.assertEquals("pero", person1.name);
                                return updateBug7102(person.id);
                            })
                            .flatMap(v -> getBug7102(person.id))
                            .map(person2 -> {
                                Assertions.assertEquals("jozo", person2.name);
                                return null;
                            });
                }).flatMap(v -> Panache.withTransaction(() -> Person.deleteAll())));
    }

    @WithTransaction
    Uni<Person> createBug7102() {
        Person personPanache = new Person();
        personPanache.name = "pero";
        return personPanache.persistAndFlush().map(v -> personPanache);
    }

    @WithTransaction
    Uni<Void> updateBug7102(Long id) {
        return Person.<Person> findById(id)
                .map(person -> {
                    person.name = "jozo";
                    return null;
                });
    }

    @WithSession
    Uni<Person> getBug7102(Long id) {
        return Person.findById(id);
    }

    @DisabledOnIntegrationTest
    @TestReactiveTransaction
    @Test
    @Order(100)
    public void testTestTransaction(UniAsserter asserter) {
        asserter.assertNotNull(() -> Panache.currentTransaction());
        asserter.assertEquals(() -> Person.count(), 0l);
        asserter.assertNotNull(() -> new Person().persist());
        asserter.assertEquals(() -> Person.count(), 1l);
    }

    @DisabledOnIntegrationTest
    @TestReactiveTransaction
    @Test
    @Order(101)
    public void testTestTransaction2(UniAsserter asserter) {
        asserter.assertNotNull(() -> Panache.currentTransaction());
        // make sure the previous one was rolled back
        asserter.assertEquals(() -> Person.count(), 0l);
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    @Order(200)
    public void testReactiveTransactional(UniAsserter asserter) {
        asserter.assertEquals(() -> reactiveTransactional(), 1l);
    }

    @WithTransaction
    Uni<Long> reactiveTransactional() {
        return Panache.currentTransaction()
                .invoke(tx -> assertNotNull(tx))
                .chain(tx -> Person.count())
                .invoke(count -> assertEquals(0l, count))
                .call(() -> new Person().persist())
                .chain(tx -> Person.count());
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    @Order(201)
    public void testReactiveTransactional2(UniAsserter asserter) {
        asserter.assertTrue(() -> reactiveTransactional2());
    }

    @WithTransaction
    Uni<Boolean> reactiveTransactional2() {
        return Panache.currentTransaction()
                .invoke(tx -> assertNotNull(tx))
                .chain(tx -> Person.count())
                .invoke(count -> assertEquals(1l, count))
                .chain(() -> Person.deleteAll())
                .invoke(count -> assertEquals(1l, count))
                .chain(() -> Panache.currentTransaction())
                .invoke(tx -> tx.markForRollback())
                .map(tx -> true);
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    @Order(202)
    public void testReactiveTransactional3(UniAsserter asserter) {
        asserter.assertEquals(() -> testReactiveTransactional3(), 1l);
    }

    @ReactiveTransactional
    Uni<Long> testReactiveTransactional3() {
        return Panache.currentTransaction()
                .invoke(tx -> assertNotNull(tx))
                .chain(tx -> Person.count())
                // make sure it was rolled back
                .invoke(count -> assertEquals(1l, count))
                .call(() -> Person.deleteAll());
    }

    @DisabledOnIntegrationTest
    @RunOnVertxContext
    @Test
    @Order(300)
    public void testPersistenceException(UniAsserter asserter) {
        asserter.assertFailedWith(() -> Panache.withTransaction(() -> new Person().delete()), PersistenceException.class);
    }

    @Test
    public void testBeerRepository() {
        RestAssured.when().get("/test-repo/beers").then().body(is("OK"));
    }

}
