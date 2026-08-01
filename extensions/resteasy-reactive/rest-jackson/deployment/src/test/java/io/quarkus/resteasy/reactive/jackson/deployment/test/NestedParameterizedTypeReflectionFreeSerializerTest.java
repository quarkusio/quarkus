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

public class NestedParameterizedTypeReflectionFreeSerializerTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(NestedParameterizedTypeResource.class,
                                    NestedParameterizedTypeResource.ServiceStatus.class,
                                    NestedParameterizedTypeResource.MapOfListsRequest.class,
                                    NestedParameterizedTypeResource.ListOfListsRequest.class,
                                    NestedParameterizedTypeResource.ListOfMapsRequest.class,
                                    NestedParameterizedTypeResource.OptionalListRequest.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testMapOfLists() {
        RestAssured
                .with()
                .body("{\"mapOfLists\":{\"group\":[{\"name\":\"alpha\"}]}}")
                .contentType("application/json")
                .post("/nested-parameterized/map-of-lists")
                .then()
                .statusCode(200)
                .body(is("alpha"));
    }

    @Test
    public void testListOfLists() {
        RestAssured
                .with()
                .body("{\"listOfLists\":[[{\"name\":\"beta\"}]]}")
                .contentType("application/json")
                .post("/nested-parameterized/list-of-lists")
                .then()
                .statusCode(200)
                .body(is("beta"));
    }

    @Test
    public void testListOfMaps() {
        RestAssured
                .with()
                .body("{\"listOfMaps\":[{\"key\":{\"name\":\"gamma\"}}]}")
                .contentType("application/json")
                .post("/nested-parameterized/list-of-maps")
                .then()
                .statusCode(200)
                .body(is("gamma"));
    }

    @Test
    public void testOptionalList() {
        RestAssured
                .with()
                .body("{\"optionalList\":[{\"name\":\"delta\"}]}")
                .contentType("application/json")
                .post("/nested-parameterized/optional-list")
                .then()
                .statusCode(200)
                .body(is("delta"));
    }
}
