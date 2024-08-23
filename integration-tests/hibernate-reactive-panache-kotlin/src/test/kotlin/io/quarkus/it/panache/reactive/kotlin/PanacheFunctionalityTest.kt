package io.quarkus.it.panache.reactive.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional
import io.quarkus.test.TestReactiveTransaction
import io.quarkus.test.junit.DisabledOnIntegrationTest
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.restassured.RestAssured.given
import io.restassured.RestAssured.`when`
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import jakarta.json.bind.JsonbBuilder
import jakarta.persistence.PersistenceException
import java.util.function.Supplier
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
open class PanacheFunctionalityTest {
    /**
     * Tests that direct use of the entity in the test class does not break transformation
     *
     * see https://github.com/quarkusio/quarkus/issues/1724
     */
    var p: Person = Person()

    @Test
    fun testPanacheFunctionality() {
        `when`()["/test/model-dao"].then().statusCode(`is`(200)).body(`is`("OK"))
        `when`()["/test/model"].then().statusCode(`is`(200)).body(`is`("OK"))
        `when`()["/test/model1"].then().statusCode(`is`(200)).body(`is`("OK"))
        `when`()["/test/model2"].then().statusCode(`is`(200)).body(`is`("OK"))
        `when`()["/test/projection1"].then().statusCode(`is`(200)).body(`is`("OK"))
        `when`()["/test/projection2"].then().statusCode(`is`(200)).body(`is`("OK"))
        `when`()["/test/model3"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testPanacheSerialisation() {
        given()
            .accept(ContentType.JSON)
            .`when`()["/test/ignored-properties"]
            .then()
            .body(
                `is`(
                    "{\"id\":666,\"dogs\":[],\"name\":\"Eddie\",\"serialisationTrick\":1,\"status\":\"DECEASED\"}"
                )
            )
    }

    @Test
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testPanacheInTest(asserter: UniAsserter) {
        asserter.assertEquals({ Panache.withSession { Person.count() } }, 0L)
    }

    @Test
    fun testBug5274() {
        `when`()["/test/5274"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testBug5885() {
        `when`()["/test/5885"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    /**
     * _PanacheEntityBase_ has the method _isPersistent_. This method is used by Jackson to
     * serialize the attribute *persistent* in the JSON which is not intended. This test ensures
     * that the attribute *persistent* is not generated when using Jackson.
     *
     * This test does not interact with the Quarkus application itself. It is just using the Jackson
     * ObjectMapper with a PanacheEntity. Thus this test is disabled in native mode. The test code
     * runs the JVM and not native.
     */
    @Test
    @DisabledOnIntegrationTest
    fun jacksonDeserializationIgnoresPersistentAttribute() {
        val person = Person()
        person.name = "max"
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()
        val personAsString = objectMapper.writeValueAsString(person)
        assertEquals(
            "{\"id\":null,\"name\":\"max\",\"uniqueName\":null,\"address\":null,\"status\":null,\"dogs\":[],\"serialisationTrick\":1}",
            personAsString
        )
    }

    /**
     * This test is disabled in native mode as there is no interaction with the quarkus integration
     * test endpoint.
     */
    @Test
    @DisabledOnIntegrationTest
    fun jsonbDeserializationHasAllFields() {
        val person = Person()
        person.name = "max"
        val jsonb = JsonbBuilder.create()
        val json = jsonb.toJson(person)
        assertEquals("{\"dogs\":[],\"name\":\"max\",\"serialisationTrick\":1}", json)
    }

    @Test
    fun testCompositeKey() {
        `when`()["/test/composite"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testBug7721() {
        `when`()["/test/7721"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testBug8254() {
        `when`()["/test/8254"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testBug9025() {
        `when`()["/test/9025"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testBug9036() {
        `when`()["/test/9036"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    fun testSortByNullPrecedence() {
        `when`()["/test/testSortByNullPrecedence"].then().statusCode(`is`(200)).body(`is`("OK"))
    }

    @Test
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testTransaction(asserter: UniAsserter) {
        asserter.assertNotNull { Panache.withTransaction { Panache.currentTransaction() } }
    }

    @Test
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testNoTransaction(asserter: UniAsserter) {
        asserter.assertNull { Panache.withSession { Panache.currentTransaction() } }
    }

    @Test
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testBug7102(asserter: UniAsserter) {
        asserter.execute {
            createBug7102()
                .flatMap { person: Person ->
                    getBug7102(person.id!!)
                        .flatMap { person1: Person ->
                            assertEquals("pero", person1.name)
                            updateBug7102(person.id!!)
                        }
                        .flatMap { _ -> getBug7102(person.id!!) }
                        .map { person2: Person ->
                            assertEquals("jozo", person2.name)
                            null
                        }
                }
                .flatMap { Panache.withSession { Person.deleteAll() } }
        }
    }

    @WithTransaction
    fun createBug7102(): Uni<Person> {
        val person = Person()
        person.name = "pero"
        return person.persistAndFlush()
    }

    @WithTransaction
    fun updateBug7102(id: Long): Uni<Void> {
        return Person.findById(id).map { person: Person? ->
            person?.name = "jozo"
            null
        }
    }

    @WithSession
    fun getBug7102(id: Long): Uni<Person> {
        return Person.findById(id).map { it!! }
    }

    @Test
    @Order(100)
    @TestReactiveTransaction
    @DisabledOnIntegrationTest
    fun testTestTransaction(asserter: UniAsserter) {
        asserter.assertNotNull { Panache.currentTransaction() }
        asserter.assertEquals({ Person.count() }, 0L)
        asserter.assertNotNull { Person().persist() }
        asserter.assertEquals({ Person.count() }, 1L)
    }

    @Test
    @Order(101)
    @TestReactiveTransaction
    @DisabledOnIntegrationTest
    fun testTestTransaction2(asserter: UniAsserter) {
        asserter.assertNotNull { Panache.currentTransaction() }
        // make sure the previous one was rolled back
        asserter.assertEquals({ Person.count() }, 0L)
    }

    @Test
    @Order(200)
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testReactiveTransactional(asserter: UniAsserter) {
        asserter.assertEquals({ reactiveTransactional() }, 1L)
    }

    @WithTransaction
    fun reactiveTransactional(): Uni<Long> {
        return Panache.currentTransaction()
            .invoke { tx -> Assertions.assertNotNull(tx) }
            .chain { tx -> Person.count() }
            .invoke { count -> assertEquals(0L, count) }
            .call(Supplier { Person().persist<Person>() })
            .chain { tx -> Person.count() }
    }

    @Test
    @Order(201)
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testReactiveTransactional2(asserter: UniAsserter) {
        asserter.assertTrue { reactiveTransactional2() }
    }

    @WithTransaction
    fun reactiveTransactional2(): Uni<Boolean> {
        return Panache.currentTransaction()
            .invoke { tx -> Assertions.assertNotNull(tx) }
            .chain(Supplier { Person.count() })
            .invoke { count -> assertEquals(1L, count) }
            .chain(Supplier { Person.deleteAll() })
            .invoke { count -> assertEquals(1L, count) }
            .chain(Supplier { Panache.currentTransaction() })
            .invoke { tx -> tx.markForRollback() }
            .map { tx -> true }
    }

    @Test
    @Order(202)
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testReactiveTransactional3(asserter: UniAsserter) {
        asserter.assertEquals({ testReactiveTransactional3() }, 1L)
    }

    @ReactiveTransactional
    fun testReactiveTransactional3(): Uni<Long> {
        return Panache.currentTransaction()
            .invoke { tx -> Assertions.assertNotNull(tx) }
            .chain { tx -> Person.count() }
            // make sure it was rolled back
            .invoke { count -> assertEquals(1L, count) }
            .call(Supplier { Person.deleteAll() })
    }

    @Test
    @Order(300)
    @RunOnVertxContext
    @DisabledOnIntegrationTest
    fun testPersistenceException(asserter: UniAsserter) {
        asserter.assertFailedWith(
            { Panache.withSession { Person().delete() } },
            PersistenceException::class.java
        )
    }
}
