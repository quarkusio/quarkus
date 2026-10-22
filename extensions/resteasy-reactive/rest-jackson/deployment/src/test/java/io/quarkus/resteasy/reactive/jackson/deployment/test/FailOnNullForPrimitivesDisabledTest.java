package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class FailOnNullForPrimitivesDisabledTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FailOnNullForPrimitivesResource.class, PrimitiveHolder.class)
                            .addAsResource(new StringAsset("quarkus.jackson.fail-on-null-for-primitives=false\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testNullForPrimitiveWithFeatureDisabled() {
        // With FAIL_ON_NULL_FOR_PRIMITIVES=false (default), null for primitives should default to 0
        given()
                .body("{\"name\":\"test\",\"count\":null}")
                .contentType("application/json")
                .when()
                .post("/fail-on-null/primitive")
                .then()
                .statusCode(200)
                .body("name", is("test"))
                .body("count", is(0));
    }
}
