package io.quarkus.it.panache;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.DisabledOnNativeImage;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Test various Panache operations running in Quarkus
 */
@QuarkusTest
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
        RestAssured.when().get("/test/model3").then().body(is("OK"));
    }

    @Test
    public void testPanacheSerialisation() {
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/test/ignored-properties")
                .then()
                .body(is("{\"id\":666,\"dogs\":[],\"name\":\"Eddie\",\"serialisationTrick\":1,\"status\":\"DECEASED\"}"));
        RestAssured.given().accept(ContentType.XML)
                .when().get("/test/ignored-properties")
                .then().body(is(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><id>666</id><name>Eddie</name><serialisationTrick>1</serialisationTrick><status>DECEASED</status></person>"));
    }

    @DisabledOnNativeImage
    @Test
    public void testPanacheInTest() {
        Assertions.assertEquals(0, Person.count());
    }

    @Test
    public void testBug5274() {
        RestAssured.when().get("/test/5274").then().body(is("OK"));
    }

    @Test
    public void testBug5885() {
        RestAssured.when().get("/test/5885").then().body(is("OK"));
    }

    @Test
    public void testJaxbAnnotationTransfer() {
        RestAssured.when()
                .get("/test/testJaxbAnnotationTransfer")
                .then()
                .body(is("OK"));
    }

    /**
     * This test is disabled in native mode as there is no interaction with the quarkus integration test endpoint.
     */
    @DisabledOnNativeImage
    @Test
    public void jacksonDeserilazationIgnoresPersitantAttribute() throws JsonProcessingException {
        // set Up
        Person person = new Person();
        person.name = "max";
        // do
        ObjectMapper objectMapper = new ObjectMapper();
        String personAsString = objectMapper.writeValueAsString(person);
        // check 
        // hence no 'persistence'-attribute
        assertEquals(
                "{\"id\":null,\"name\":\"max\",\"uniqueName\":null,\"address\":null,\"status\":null,\"dogs\":[],\"serialisationTrick\":1}",
                personAsString);
    }
}
