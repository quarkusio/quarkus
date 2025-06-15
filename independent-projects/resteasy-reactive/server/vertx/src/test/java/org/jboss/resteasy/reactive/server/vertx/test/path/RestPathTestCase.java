package org.jboss.resteasy.reactive.server.vertx.test.path;

import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class RestPathTestCase {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .addScanCustomizer(new Consumer<ResteasyReactiveDeploymentManager.ScanStep>() {
                @Override
                public void accept(ResteasyReactiveDeploymentManager.ScanStep scanStep) {
                    scanStep.setApplicationPath("/foo");
                }
            }).withApplicationRoot((jar) -> jar.addClass(HelloResource.class));

    @Test
    public void testRestPath() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/foo/hello/nested").then().statusCode(200).body(Matchers.is("world hello"));
        RestAssured.when().get("/foo/helloX").then().statusCode(404);
        RestAssured.when().get("/foo/hello").then().statusCode(200).body(Matchers.is("hello"));
    }

    @Test
    public void testListOfPathParams() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/foo/hello/other/bar/baz/boo/bob").then().statusCode(200)
                .body(Matchers.is("[bar, baz, boo, bob]"));
    }
}
