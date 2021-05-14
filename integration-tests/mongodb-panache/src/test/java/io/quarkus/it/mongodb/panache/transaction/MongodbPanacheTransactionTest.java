package io.quarkus.it.mongodb.panache.transaction;

import static io.restassured.RestAssured.get;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(MongoReplicaSetTestResource.class)
@DisabledOnOs(OS.WINDOWS)
class MongodbPanacheTransactionTest {
    private static final TypeRef<List<PersonDTO>> LIST_OF_PERSON_TYPE_REF = new TypeRef<List<PersonDTO>>() {
    };

    @Test
    public void testTheEndpoint() {
        String endpoint = "/transaction";
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)));

        //delete all
        Response response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        List<PersonDTO> list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(0, list.size());

        PersonDTO person1 = new PersonDTO();
        person1.id = 1L;
        person1.firstname = "John";
        person1.lastname = "Doe";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person1)
                .post(endpoint)
                .andReturn();
        Assertions.assertEquals(201, response.statusCode());

        PersonDTO person2 = new PersonDTO();
        person2.id = 2L;
        person2.firstname = "Jane";
        person2.lastname = "Doh!";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person2)
                .post(endpoint)
                .andReturn();
        Assertions.assertEquals(201, response.statusCode());

        list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(2, list.size());

        // This will insert Charles Baudelaire then throws an exception.
        // As we are in a transaction Charles Baudelaire will not be saved.
        PersonDTO person3 = new PersonDTO();
        person3.id = 3L;
        person3.firstname = "Charles";
        person3.lastname = "Baudelaire";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person3)
                .post(endpoint + "/exception")
                .andReturn();
        Assertions.assertEquals(500, response.statusCode());

        list = get(endpoint).as(LIST_OF_PERSON_TYPE_REF);
        Assertions.assertEquals(2, list.size());

        //count
        Long count = get(endpoint + "/count").as(Long.class);
        Assertions.assertEquals(2, count);

        //update a person
        person2.lastname = "Doe";
        response = RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(person2)
                .put(endpoint)
                .andReturn();
        Assertions.assertEquals(202, response.statusCode());

        //check that the title has been updated
        person2 = get(endpoint + "/" + person2.id.toString()).as(PersonDTO.class);
        Assertions.assertEquals(2L, person2.id);
        Assertions.assertEquals("Doe", person2.lastname);

        //rename the Doe
        response = RestAssured
                .given()
                .queryParam("previousName", "Doe").queryParam("newName", "Dupont")
                .header("Content-Type", "application/json")
                .when().post(endpoint + "/rename")
                .andReturn();
        Assertions.assertEquals(200, response.statusCode());

        //delete a person
        response = RestAssured
                .given()
                .delete(endpoint + "/" + person2.id.toString())
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        count = get(endpoint + "/count").as(Long.class);
        Assertions.assertEquals(1, count);

        //delete all
        response = RestAssured
                .given()
                .delete(endpoint)
                .andReturn();
        Assertions.assertEquals(204, response.statusCode());

        count = get(endpoint + "/count").as(Long.class);
        Assertions.assertEquals(0, count);
    }

}
