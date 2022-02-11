package com.example.postgresql.devservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class PersonResourceTest {

    @BeforeEach
    @Transactional
    public void setUp() {
        var personOne = new Person();
        personOne.age = 27;
        personOne.name = "netodevel";
        personOne.persist();

        var otherPerson = new Person();
        otherPerson.age = 27;
        otherPerson.name = "fake_name";
        otherPerson.persist();
    }

    @AfterEach
    @Transactional
    public void tearDown() {
        Person.deleteAll();
    }

    @Test
    @DisplayName("given the use of devservices then it should a return a list of persons")
    public void shouldReturnListPersons() {
        List<Person> personList = RestAssured.get("/persons")
                .then()
                .extract().jsonPath().getList(".");
        assertEquals(2, personList.size());
    }
}
