package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetryDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSpanExporter.class, TracerRouter.class, HelloResource.class)
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties("")), "application.properties"));

    @Test
    void testDevMode() {
        //make sure we have the correct span in dev mode
        //and the hot replacement stuff is not messing things up
        RestAssured.when().get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        TEST.modifySourceFile(TracerRouter.class, s -> s.replace("Hello", "Goodbye"));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Goodbye Tracer!"));
    }
}
