package io.quarkus.it.panache

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.it.panache.kotlin.Dog
import io.quarkus.it.panache.kotlin.Person
import io.quarkus.test.junit.DisabledOnNativeImage
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
open class KotlinPanacheFunctionalityTest {

    @Test
    fun testPanacheFunctionality() {
        RestAssured.`when`()["/test/model-dao"].then().body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/model"].then().body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/accessors"].then().body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/model1"].then().body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/model2"].then().body(Matchers.`is`("OK"))
        RestAssured.`when`()["/test/model3"].then().body(Matchers.`is`("OK"))
    }

    @Test
    fun testPanacheSerialisation() {
        RestAssured.given().accept(ContentType.JSON)
                .`when`()["/test/ignored-properties"]
                .then()
                .body(Matchers.`is`("{\"id\":666,\"dogs\":[],\"name\":\"Eddie\",\"serialisationTrick\":1,\"status\":\"DECEASED\"}"))
        RestAssured.given().accept(ContentType.XML)
                .`when`()["/test/ignored-properties"]
                .then().body(Matchers.`is`(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><id>666</id><name>Eddie</name><serialisationTrick>1</serialisationTrick><status>DECEASED</status></person>"))
    }

    /**
     * This test is disabled in native mode as there is no interaction with the quarkus integration test endpoint.
     */
    @Test
    @DisabledOnNativeImage
    @Throws(JsonProcessingException::class)
    fun jacksonDeserializationIgnoresPersistentAttribute() { // set Up
        val person = Person()
        person.name = "max"
        // do
        val objectMapper = ObjectMapper()
        val personAsString = objectMapper.writeValueAsString(person)
        // check
// hence no 'persistence'-attribute
        Assertions.assertEquals(
                "{\"id\":null,\"name\":\"max\",\"uniqueName\":null,\"address\":null,\"status\":null,\"dogs\":[],\"serialisationTrick\":1}",
                personAsString)
    }

    @Test
    fun testBug9036() {
        RestAssured.`when`()["/test/9036"].then().body(Matchers.`is`("OK"))
    }

    @Test
    fun entityManagerIsInjected() {
        assertNotNull(Dog.getEntityManager())
    }

    @Test
    fun testBug19420() {
        RestAssured.`when`()["/test/19420"].then().body(Matchers.`is`("OK"))
    }
}