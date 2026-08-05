package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TooManyParamsMultipartFormInputTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            })
            .overrideRuntimeConfigKey("quarkus.http.limits.max-parameters", "3");

    @Test
    public void exceedingLimit() {
        given()
                .multiPart("p1", "v1")
                .multiPart("p2", "v2")
                .multiPart("p3", "v3")
                .multiPart("p4", "v4")
                .accept("text/plain")
                .when()
                .post("/test")
                .then()
                .statusCode(400);
    }

    @Test
    public void withinLimit() {
        given()
                .multiPart("p1", "v1")
                .multiPart("p2", "v2")
                .multiPart("p3", "v3")
                .accept("text/plain")
                .when()
                .post("/test")
                .then()
                .statusCode(200);
    }

    @Path("/test")
    public static class Resource {

        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@RestForm String p1) {
            return p1;
        }
    }
}
