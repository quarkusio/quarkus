package io.quarkus.hibernate.reactive.rest.data.panache.deployment.build;

import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class BuildConditionsWithResourceEnabledTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("collections.endpoint", "enable")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsResource.class));

    @Test
    void shouldResourceBeFound() {
        given().accept("application/json")
                .when().get("/collections")
                .then().statusCode(200);
    }
}
