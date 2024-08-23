package io.quarkus.resteasy.jaxb.deployment;

import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class ProducesXMLTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Bar.class, FooResource.class));

    @Test
    public void testProducesXML() {
        final Bar res = RestAssured.given()
                .body("open bar")
                .contentType(ContentType.TEXT)
                .when().post("/foo")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_XML)
                .extract().as(Bar.class);
        Assertions.assertEquals(new Bar("open", "bar"), res);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .when().get("/foo")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_XML)
                .body("bars.bar.size()", is(2));
    }

    @Path("/foo")
    public static class FooResource {

        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_XML)
        public Bar post(String bar) {
            final String[] s = bar.split(" ");
            return new Bar(s[0], s[1]);
        }

        @GET
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.APPLICATION_XML)
        @Wrapped(element = "bars")
        public List<Bar> list() {
            return Arrays.asList(new Bar("name_1", "description_1"),
                    new Bar("name_2", "description_2"));
        }
    }
}
