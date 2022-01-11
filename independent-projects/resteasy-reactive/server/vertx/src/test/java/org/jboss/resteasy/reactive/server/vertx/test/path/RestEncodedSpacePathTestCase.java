package org.jboss.resteasy.reactive.server.vertx.test.path;

import io.restassured.RestAssured;
import java.util.function.Consumer;
import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.processor.ResteasyReactiveDeploymentManager;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RestEncodedSpacePathTestCase {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .addScanCustomizer(new Consumer<ResteasyReactiveDeploymentManager.ScanStep>() {
                @Override
                public void accept(ResteasyReactiveDeploymentManager.ScanStep scanStep) {
                    scanStep.setApplicationPath("/bar");
                }
            })
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloEncodedSpaceResource.class));

    @Test
    public void testRestEncodedSpacePath() {
        RestAssured.basePath = "/";
        RestAssured.when().get("/bar/hello test").then().statusCode(200).body(Matchers.is("hello"));
        RestAssured.when().get("/bar/hello test/nested test").then().statusCode(200).body(Matchers.is("world hello"));
    }
}
