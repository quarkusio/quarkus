package org.jboss.resteasy.reactive.server.vertx.test.path;

import io.restassured.RestAssured;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class WhitespaceInPathTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class));

    @Test
    public void test() {
        RestAssured.when().get("/hello dear world/ yolo /foo").then().statusCode(200)
                .body(Matchers.is("yolo foo"));
    }

    @Path("hello dear world")
    public static class HelloResource {

        @Path(" yolo /{name}")
        @GET
        public String hello(String name) {
            return "yolo " + name;
        }
    }
}
