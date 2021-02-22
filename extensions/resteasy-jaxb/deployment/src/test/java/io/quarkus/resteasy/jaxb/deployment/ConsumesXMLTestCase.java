package io.quarkus.resteasy.jaxb.deployment;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class ConsumesXMLTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Bar.class, FooResource.class));

    @Test
    public void testConsumesXML() {
        RestAssured.given()
                .body(new Bar("open", "bar"))
                .contentType(ContentType.XML)
                .when().post("/foo")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body(Matchers.is("open bar"));
    }

    @Path("/foo")
    public static class FooResource {

        @POST
        @Consumes(MediaType.APPLICATION_XML)
        @Produces(MediaType.TEXT_PLAIN)
        public String post(Bar bar) {
            return bar.name + " " + bar.description;
        }
    }

}
