package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EmployeeResourceTest {

    @Test
    public void testFindEmployeesByOrganizationalUnit() {
        List<Employee> employees = when().get("/employee/unit/Delivery Unit").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Employee.class);

        assertThat(employees).extracting("userId").containsExactlyInAnyOrder("johdoe", "petdig");
    }

}
