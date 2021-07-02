package io.quarkus.it.resteasy.reactive;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.it.resteasy.reactive.generics.ExtendedClass;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GenericsResourceTest {

    @Test
    public void testExtendedEndpoint() {
        ExtendedClass response = given()
                .when().get("/generics")
                .then()
                .statusCode(200)
                .extract().body().as(ExtendedClass.class);
        assertEquals("myBaseVariable", response.getBaseVariable());
        assertEquals("myExtendedVariable", response.getExtendedVariable());
        assertEquals("myData", response.getData().getDataVariable());
    }
}
