package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MalformedFormUrlencodedBodyTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MalformedFormResource.class);
                }

            });

    @Test
    public void testMalformedPercentEncodingInValue() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .body("name=%zz")
                .when()
                .post("/form")
                .then()
                .statusCode(400);
    }

    @Test
    public void testMalformedPercentEncodingInName() {
        given()
                .contentType("application/x-www-form-urlencoded")
                .body("%zz=value")
                .when()
                .post("/form")
                .then()
                .statusCode(400);
    }

    @Path("/form")
    public static class MalformedFormResource {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String form(@FormParam("name") String name) {
            return "ok";
        }
    }
}
