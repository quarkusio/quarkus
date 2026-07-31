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
import io.restassured.response.ValidatableResponse;

public class NestedParameterizedTypeReflectionFreeSerializerTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(NestedParameterizedTypeResource.class,
                                    NestedParameterizedTypeResource.ServiceStatus.class,
                                    NestedParameterizedTypeResource.Request.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testMapOfLists() {
        // Map<String, List<ServiceStatus>> must keep the inner List type argument; otherwise Jackson
        // deserializes the elements as LinkedHashMaps and dereferencing them fails
        post("/nested-parameterized-type/map-of-lists",
                "{\"mapOfLists\":{\"group\":[{\"name\":\"alpha\"}]}}")
                .body(is("alpha"));
    }

    @Test
    public void testListOfLists() {
        post("/nested-parameterized-type/list-of-lists",
                "{\"listOfLists\":[[{\"name\":\"beta\"}]]}")
                .body(is("beta"));
    }

    @Test
    public void testListOfMaps() {
        post("/nested-parameterized-type/list-of-maps",
                "{\"listOfMaps\":[{\"key\":{\"name\":\"gamma\"}}]}")
                .body(is("gamma"));
    }

    @Test
    public void testOptionalOfList() {
        post("/nested-parameterized-type/optional-of-list",
                "{\"optionalOfList\":[{\"name\":\"delta\"}]}")
                .body(is("delta"));
    }

    private static ValidatableResponse post(String path, String body) {
        return RestAssured
                .with()
                .body(body)
                .contentType("application/json; charset=utf-8")
                .post(path)
                .then()
                .statusCode(200);
    }
}
