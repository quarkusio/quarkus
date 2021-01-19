package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CarResourceTest {

    @Test
    void testFindByBrand() {
        final List<Car> cars = when().get("/car/brand/Rinspeed").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Car.class);
        assertThat(cars).hasSize(2);
    }

    @Test
    void findByBrandAndModel() {
        final Car car = when().get("/car/brand/Rinspeed/model/Oasis").then()
                .statusCode(200)
                .extract().body().jsonPath().getObject(".", Car.class);
        assertThat(car).extracting(Car::getBrand).isEqualTo("Rinspeed");
        assertThat(car).extracting(Car::getModel).isEqualTo("Oasis");
    }

    @Test
    void findById() {
        final Car car = when().get("/car/1").then()
                .statusCode(200)
                .extract().body().jsonPath().getObject(".", Car.class);
        assertThat(car).extracting(Car::getBrand).isEqualTo("Monteverdi");
        assertThat(car).extracting(Car::getModel).isEqualTo("Hai 450");
    }

    @Test
    void findModelsByBrand() {
        final List<String> models = when().get("/car/brand/Rinspeed/models").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", String.class);
        assertThat(models).contains("iChange", "Oasis");
    }
}
