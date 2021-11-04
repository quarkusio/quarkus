package io.quarkus.resteasy.reactive.server.test.path;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RestApplicationPathTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withConfigurationResource("empty.properties")
            .overrideConfigKey("quarkus.resteasy-reactive.path", "/foo")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, BarApp.class, BaseApplication.class));

    /**
     * Using @ApplicationPath will overlay/replace `quarkus.rest.path`.
     * Per spec:
     * <quote>
     * Identifies the application path that serves as the base URI for all resource
     * URIs provided by Path. May only be applied to a subclass of Application.
     * </quote>
     *
     * This path will also be relative to the configured HTTP root
     */
    @ApplicationPath("/bar")
    public static class BarApp extends BaseApplication {
    }

    public static abstract class BaseApplication extends Application {
    }

    @Test
    public void testRestPath() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/app/bar/hello").then().body(Matchers.is("hello"));
        RestAssured.when().get("/app/bar/hello/nested").then().body(Matchers.is("world hello"));
    }
}
