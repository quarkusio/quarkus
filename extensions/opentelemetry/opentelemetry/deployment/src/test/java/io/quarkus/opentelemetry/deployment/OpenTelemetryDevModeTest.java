package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.testrunner.ContinuousTestingTestUtils;
import io.restassured.RestAssured;

public class OpenTelemetryDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TracerRouter.class)
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties("")), "application.properties"));

    @Test
    void testDevMode() {
        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        TEST.modifySourceFile(TracerRouter.class, s -> s.replace("Hello", "Goodbye"));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Goodbye Tracer!"));
    }
}
