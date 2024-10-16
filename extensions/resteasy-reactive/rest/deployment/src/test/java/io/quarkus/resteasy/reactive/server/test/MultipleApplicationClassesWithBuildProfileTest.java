package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

class MultipleApplicationClassesWithBuildProfileTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            Application1.class, Application2.class, TestResource.class));

    @Test
    public void testNoAnnotation() {
        get("/1/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("test"));
    }

    @ApplicationPath("1")
    public static class Application1 extends Application {

    }

    @ApplicationPath("2")
    @IfBuildProperty(name = "some.prop2", stringValue = "v2")
    public static class Application2 extends Application {

    }

    @Path("test")
    public static class TestResource {

        @GET
        public String get() {
            return "test";
        }
    }
}
