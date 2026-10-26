package io.quarkus.resteasy.reactive.server.servlet.test;

import static io.restassured.RestAssured.get;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class ServletApplicationPathTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(App.class, TestResource.class));

    @Test
    public void testServletMapping() {
        get("/app/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("test"));
        get("/test")
                .then()
                .statusCode(404);
    }

    @ApplicationPath("app")
    public static class App extends Application {
    }

    @Path("test")
    public static class TestResource {

        @GET
        public String get() {
            return "test";
        }
    }
}
