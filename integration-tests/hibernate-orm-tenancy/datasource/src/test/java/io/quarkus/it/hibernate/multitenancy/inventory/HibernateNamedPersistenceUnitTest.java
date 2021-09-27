package io.quarkus.it.hibernate.multitenancy.inventory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.ObjectMapperType;

@QuarkusTest
public class HibernateNamedPersistenceUnitTest {

    private static RestAssuredConfig config;

    @BeforeAll
    public static void beforeClass() {
        config = RestAssured.config().objectMapperConfig(new ObjectMapperConfig(ObjectMapperType.JSONB));
    }

    @Test
    public void testGetPlanesDefaultTenant() throws Exception {

        Plane[] planes = given().config(config).when().get("/inventory").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Plane[].class);
        assertThat(planes, arrayContaining(new Plane(1L, "Airbus A320"), new Plane(2L, "Airbus A350")));

    }

    @Test
    public void testGetPlanesTenantMycompany() throws Exception {

        Plane[] planes = given().config(config).when().get("/mycompany/inventory").then().assertThat()
                .statusCode(is(Status.OK.getStatusCode())).extract()
                .as(Plane[].class);
        assertThat(planes, arrayContaining(new Plane(1L, "Boeing 737"), new Plane(2L, "Boeing 747")));

    }
}
