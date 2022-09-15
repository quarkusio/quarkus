package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RequestScopedParamConverterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class, Model.class);
                }
            });

    @Test
    public void testNoAnnotation() {
        given().header("foo", "bar").when().get("/test/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("test/bar"));
    }

    @Path("test")
    public static class TestResource {
        @GET
        @Path("{value}")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@PathParam("value") Model model) {
            return model.value + "/" + model.fooHeader;
        }
    }

    public static class Model {

        public final String value;
        public final String fooHeader;

        public Model(String value, String fooHeader) {
            this.value = value;
            this.fooHeader = fooHeader;
        }

        // called automatically by RR based on the JAX-RS convention
        public static Model valueOf(String value) {
            return new Model(value, (String) CurrentRequestManager.get().getHeader("foo", true));
        }

    }

}
