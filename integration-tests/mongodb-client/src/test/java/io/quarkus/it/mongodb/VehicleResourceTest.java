package io.quarkus.it.mongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.it.mongodb.discriminator.Car;
import io.quarkus.it.mongodb.discriminator.Moto;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
public class VehicleResourceTest {
    @BeforeEach
    public void clearCollection() {
        Response response = RestAssured
                .given()
                .delete("/vehicles")
                .andReturn();
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void testVehicleEndpoint() {
        Car car = new Car("CAR", "Renault Clio", 5);
        given().header("Content-Type", "application/json").body(car)
                .when().post("/vehicles")
                .then().statusCode(201);

        Moto moto = new Moto("MOTO", "Harley Davidson Sportster", false);
        given().header("Content-Type", "application/json").body(moto)
                .when().post("/vehicles")
                .then().statusCode(201);

        given().get("/vehicles").then().statusCode(200).body("size()", is(2));
    }
}
