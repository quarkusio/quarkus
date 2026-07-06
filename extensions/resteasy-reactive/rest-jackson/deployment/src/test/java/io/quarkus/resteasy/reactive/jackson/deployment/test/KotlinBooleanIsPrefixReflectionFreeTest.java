package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class KotlinBooleanIsPrefixReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(KotlinBooleanIsPrefixDto.class,
                                    KotlinBooleanIsPrefixResource.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testSerializationUsesKotlinPropertyName() {
        RestAssured.get("/kotlin-boolean-is-prefix")
                .then()
                .statusCode(200)
                .body("isEligible", is(true))
                .body(not(containsString("\"eligible\"")));
    }

    @Test
    public void testRoundTrip() {
        RestAssured
                .with()
                .body("{\"isEligible\": true}")
                .contentType("application/json")
                .post("/kotlin-boolean-is-prefix")
                .then()
                .statusCode(200)
                .body("isEligible", is(true));
    }
}
