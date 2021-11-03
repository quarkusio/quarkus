package io.quarkus.resteasy.reactive.server.test.path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RestPathTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withConfigurationResource("empty.properties")
            .overrideConfigKey("quarkus.resteasy-reactive.path", "/foo")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class));

    @Test
    public void testRestPath() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/app/foo/hello").then().statusCode(200).body(Matchers.is("hello"));
        RestAssured.when().get("/app/foo/hello/nested").then().statusCode(200).body(Matchers.is("world hello"));
    }

    @Test
    public void testListOfPathParams() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/app/foo/hello/other/bar/baz/boo/bob").then().statusCode(200)
                .body(Matchers.is("[bar, baz, boo, bob]"));
    }
}
