package io.quarkus.it.hibernate.multitenancy.inventory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HibernateNamedPersistenceUnitTest {

    @Test
    public void testGetPlanesDefaultTenant() throws Exception {

        Plane[] planes = given().when().get("/inventory").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Plane[].class);
        assertThat(planes, arrayContaining(new Plane(1L, "Airbus A320"), new Plane(2L, "Airbus A350")));

    }

    @Test
    public void testGetPlanesTenantMycompany() throws Exception {

        Plane[] planes = given().when().get("/mycompany/inventory").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Plane[].class);
        assertThat(planes, arrayContaining(new Plane(1L, "Boeing 737"), new Plane(2L, "Boeing 747")));

    }
}
