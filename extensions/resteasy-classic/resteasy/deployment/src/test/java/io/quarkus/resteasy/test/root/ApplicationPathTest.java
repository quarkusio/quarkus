package io.quarkus.resteasy.test.root;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ApplicationPathTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, HelloApp.class));

    @Test
    public void testResources() {
        RestAssured.when().get("/hello/world").then().body(Matchers.is("hello world"));
        RestAssured.when().get("/world").then().statusCode(404);
    }

    @Path("world")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello world";
        }

    }

    @ApplicationPath("hello")
    public static class HelloApp extends Application {

    }

}
