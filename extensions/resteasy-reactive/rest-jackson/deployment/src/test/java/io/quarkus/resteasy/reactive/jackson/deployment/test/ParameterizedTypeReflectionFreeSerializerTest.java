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

public class ParameterizedTypeReflectionFreeSerializerTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ParameterizedTypeResource.class,
                                    ParameterizedTypeResource.Difficulty.class,
                                    ParameterizedTypeResource.ChildDto.class,
                                    ParameterizedTypeResource.Holder.class,
                                    ParameterizedTypeResource.ThingRequest.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testGenericWrapperEnumDeserialization() {
        // Holder<Difficulty> must preserve the type parameter so the enum
        // deserializes correctly, not as a raw String (ClassCastException)
        RestAssured
                .with()
                .body("{\"difficulty\":{\"value\":\"HARD\"},\"child\":{\"value\":{\"label\":\"x\"}}}")
                .contentType("application/json; charset=utf-8")
                .post("/parameterized-type")
                .then()
                .statusCode(200)
                .body("difficulty", is("HARD"));
    }

    @Test
    public void testGenericWrapperDtoDeserialization() {
        // Holder<ChildDto> must preserve the type parameter so the nested DTO
        // deserializes correctly, not as a LinkedHashMap (ClassCastException)
        RestAssured
                .with()
                .body("{\"difficulty\":{\"value\":\"EASY\"},\"child\":{\"value\":{\"label\":\"hello\"}}}")
                .contentType("application/json; charset=utf-8")
                .post("/parameterized-type")
                .then()
                .statusCode(200)
                .body("child", is("hello"));
    }

    @Test
    public void testGenericWrapperAbsentField() {
        RestAssured
                .with()
                .body("{}")
                .contentType("application/json; charset=utf-8")
                .post("/parameterized-type")
                .then()
                .statusCode(200)
                .body("difficulty", is("absent"),
                        "child", is("absent"));
    }
}
