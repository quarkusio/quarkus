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

public class KotlinJsonPropertyReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(KotlinJsonPropertyDto.class,
                                    KotlinJsonPropertyResource.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testSerializationUsesJsonPropertyName() {
        RestAssured.get("/kotlin-json-property")
                .then()
                .statusCode(200)
                .body("name", is("Alice"))
                .body(not(containsString("\"field\"")));
    }

    @Test
    public void testRoundTrip() {
        RestAssured
                .with()
                .body("{\"name\": \"Bob\"}")
                .contentType("application/json")
                .post("/kotlin-json-property")
                .then()
                .statusCode(200)
                .body("name", is("Bob"))
                .body(not(containsString("\"field\"")));
    }
}
