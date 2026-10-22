package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FailOnNullForPrimitivesTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FailOnNullForPrimitivesResource.class, PrimitiveHolder.class)
                            .addAsResource(new StringAsset("quarkus.jackson.fail-on-null-for-primitives=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testNullForPrimitiveWithFeatureEnabled() {
        // With FAIL_ON_NULL_FOR_PRIMITIVES=true, sending null for a primitive field should fail with 400
        given()
                .body("{\"name\":\"test\",\"count\":null}")
                .contentType("application/json")
                .when()
                .post("/fail-on-null/primitive")
                .then()
                .statusCode(400);
    }
}
