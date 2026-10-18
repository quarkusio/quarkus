package io.quarkus.it.panache.reactive;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class NamedPersistenceUnitsTest {

    @Test
    public void testPanacheFunctionality() throws Exception {
        RestAssured.when().get("/secondaryPersistenceUnit").then().body(is("mainFruit secondaryFruit"));
    }

}