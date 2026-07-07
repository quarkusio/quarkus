package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verifies that Kotlin classes with default parameter values and non-null types are
 * handled correctly when reflection-free serializers are enabled. Kotlin classes are
 * excluded from reflection-free generation and fall back to standard Jackson with
 * KotlinModule, which properly supports default values and non-null validation.
 */
public class KotlinDefaultParamsReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(KotlinDefaultParamsDto.class,
                                    KotlinDefaultParamsResource.class,
                                    KotlinModuleCustomizer.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testSerializationWithDefaults() {
        RestAssured.get("/kotlin-default-params")
                .then()
                .statusCode(200)
                .body("name", is("Alice"))
                .body("greeting", is("Hello"))
                .body("tags", empty());
    }

    @Test
    public void testDeserializationAppliesDefaults() {
        // Only sending "name" — "greeting" and "tags" should use Kotlin default values
        RestAssured
                .with()
                .body("{\"name\": \"Bob\"}")
                .contentType("application/json")
                .post("/kotlin-default-params")
                .then()
                .statusCode(200)
                .body("name", is("Bob"))
                .body("greeting", is("Hello"))
                .body("tags", empty());
    }

    @Singleton
    public static class KotlinModuleCustomizer implements ObjectMapperCustomizer {
        @Override
        public void customize(ObjectMapper mapper) {
            mapper.registerModule(new KotlinModule.Builder().build());
        }
    }
}
