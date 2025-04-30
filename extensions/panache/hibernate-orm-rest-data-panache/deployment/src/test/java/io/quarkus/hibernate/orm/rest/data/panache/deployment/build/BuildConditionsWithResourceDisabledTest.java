package io.quarkus.hibernate.orm.rest.data.panache.deployment.build;

import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BuildConditionsWithResourceDisabledTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsResource.class));

    @Test
    void shouldResourceNotBeFound() {
        given().accept("application/json")
                .when().get("/collections")
                .then().statusCode(404);
    }
}
