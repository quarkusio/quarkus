package io.quarkus.resteasy.reactive.server.test.path;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RelativeRestPathTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withConfigurationResource("empty.properties")
            .overrideConfigKey("quarkus.resteasy-reactive.path", "foo")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloResource.class));

    @Test
    public void testRestPath() {
        RestAssured.basePath = "/";
        // This is expected behavior (relative path appended to HTTP root path)
        RestAssured.when().get("/app/foo/hello").then().body(Matchers.is("hello"));
        RestAssured.when().get("/app/foo/hello/nested").then().body(Matchers.is("world hello"));
    }
}
