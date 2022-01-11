package org.jboss.resteasy.reactive.server.vertx.test.path;

import io.restassured.RestAssured;
import java.util.function.Consumer;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RestSpacePathTestCase {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .addScanCustomizer(new Consumer<ResteasyReactiveDeploymentManager.ScanStep>() {
                @Override
                public void accept(ResteasyReactiveDeploymentManager.ScanStep scanStep) {
                    scanStep.setApplicationPath("/foo");
                }
            })
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloSpaceResource.class));

    @Test
    public void testRestSpacePath() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/foo/hello test").then().statusCode(200).body(Matchers.is("hello"));
        RestAssured.when().get("/foo/hello test/nested test").then().statusCode(200).body(Matchers.is("world hello"));
    }
}
