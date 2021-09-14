package io.quarkus.it.mongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.it.mongodb.discriminator.Car;
import io.quarkus.it.mongodb.discriminator.Moto;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class VehicleResourceTest {
    @Test
    public void testVehicleEndpoint() {
        Car car = new Car();
        car.name = "Renault Clio";
        car.type = "CAR";
        car.seatNumber = 5;
        given().header("Content-Type", "application/json").body(car)
                .when().post("/vehicles")
                .then().statusCode(201);

        Moto moto = new Moto();
        moto.name = "Harley Davidson Sportster";
        moto.type = "MOTO";
        given().header("Content-Type", "application/json").body(moto)
                .when().post("/vehicles")
                .then().statusCode(201);

        given().get("/vehicles").then().statusCode(200).body("size()", is(2));
    }
}
