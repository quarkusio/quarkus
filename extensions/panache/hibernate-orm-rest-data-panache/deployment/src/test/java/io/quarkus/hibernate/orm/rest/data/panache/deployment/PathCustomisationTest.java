package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.Collection;
import io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.CollectionsController;
import io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.Item;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class PathCustomisationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsController.class, Item.class,
                            CustomPathCollectionsController.class)
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
        String name = "test-" + Objects.hash(path, accept);
        Response response = given().body("{\"name\": \"" + name + "\"}")
                .and().contentType("application/json")
                .and().accept(accept)
                .when().post(path)
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(201);
        when().delete(path + "/" + name)
                .then().statusCode(204);
    }

    @ParameterizedTest
    @ArgumentsSource(TestArgumentsProvider.class)
    void testUpdate(String path, String accept) {
        given().body("{\"name\": \"test\"}")
                .and().contentType("application/json")
                .and().accept(accept)
                .when().put(path + "/empty")
                .then().statusCode(204);
    }

    @ResourceProperties(path = "custom-collections", hal = true)
    public interface CustomPathCollectionsController extends PanacheEntityResource<Collection, String> {

        @MethodProperties(path = "api")
        javax.ws.rs.core.Response list();

        @MethodProperties(path = "api")
        Collection get(String name);

        @MethodProperties(path = "api")
        javax.ws.rs.core.Response add(Collection collection);

        @MethodProperties(path = "api")
        javax.ws.rs.core.Response update(String name, Collection collection);

        @MethodProperties(path = "api")
        void delete(String name);
    }

    static class TestArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("/collections", "application/json"),
                    Arguments.of("/custom-collections/api", "application/json"),
                    Arguments.of("/collections", "application/hal+json"),
                    Arguments.of("/custom-collections/api", "application/hal+json"));
        }
    }
}
