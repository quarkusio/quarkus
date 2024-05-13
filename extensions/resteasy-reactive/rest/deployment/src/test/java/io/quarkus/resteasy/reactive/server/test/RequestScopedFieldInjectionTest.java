package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RequestScopedFieldInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            });

    @Test
    public void test() {
        get("/test/f/p")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("field:f-parameter:p"));
    }

    @Path("/test/{field}")
    @RequestScoped
    public static class Resource {

        @RestPath
        private String field;

        @GET
        @Path("/{parameter}")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(String parameter) {
            return "field:" + field + "-parameter:" + parameter;
        }

    }
}
