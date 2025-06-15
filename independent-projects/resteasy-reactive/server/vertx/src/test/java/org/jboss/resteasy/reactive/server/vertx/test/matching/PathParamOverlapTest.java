package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PathParamOverlapTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(TestResource.class);
        }
    });

    @Test
    public void test() {
        get("/hello/some/test").then().statusCode(200).body(equalTo("Hello World!"));

        get("/hello/other/test/new").then().statusCode(200).body(equalTo("Hello other"));

        get("/hello/some/test/new").then().statusCode(200).body(equalTo("Hello some"));

        get("/hello/some/test/wrong").then().statusCode(404);

        get("/hello/other/test/wrong").then().statusCode(404);
    }

    @Path("/hello")
    public static class TestResource {

        @GET
        @Path("/some/test")
        @Produces(MediaType.TEXT_PLAIN)
        public String test() {
            return "Hello World!";
        }

        @GET
        @Path("/{id}/test/new")
        public String second(@RestPath String id) {
            return "Hello " + id;
        }
    }
}
