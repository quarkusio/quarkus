package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verifies that Kotlin classes are NOT processed by the reflection-free serializer
 * and instead fall back to standard Jackson serialization.
 */
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
    public void testKotlinClassUsesStandardJackson() {
        // Standard Jackson (without KotlinModule) strips the "is" prefix from boolean getters,
        // so isEligible() becomes "eligible". This proves the reflection-free serializer
        // (which would preserve "isEligible" from the field name) is NOT used for Kotlin classes.
        RestAssured.get("/kotlin-boolean-is-prefix")
                .then()
                .statusCode(200)
                .body("eligible", is(true));
    }

}
