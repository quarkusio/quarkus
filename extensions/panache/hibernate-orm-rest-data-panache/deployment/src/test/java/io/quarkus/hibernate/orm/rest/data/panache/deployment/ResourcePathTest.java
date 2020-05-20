package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class ResourcePathTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Item.class, ItemsResource.class, CustomPathItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testGet(String path, String accept) {
        given().accept(accept)
                .when().get(path)
                .then().statusCode(200);
    }

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testCreateAndDelete(String path, String accept) {
        Response response = given().body("{\"value\": \"test\"}")
                .and().contentType("application/json")
                .and().accept(accept)
                .when().post(path)
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(201);

        // Make sure we use the path from parameter. The location could have a path either to a standard or a custom resource
        String location = response.getHeader("Location");
        String idSegment = location.substring(location.lastIndexOf("/"));

        when().delete(path + idSegment)
                .then().statusCode(204);
    }

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testUpdate(String path, String accept) {
        given().body("{\"id\": \"1\", \"value\": \"test\"}")
                .and().contentType("application/json")
                .and().accept(accept)
                .when().put(path + "/1")
                .then().statusCode(204);
    }

    static class TestArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("/items", "application/json"),
                    Arguments.of("/custom-items/api", "application/json"),
                    Arguments.of("/items", "application/hal+json"),
                    Arguments.of("/custom-items/api", "application/hal+json"));
        }
    }
}
