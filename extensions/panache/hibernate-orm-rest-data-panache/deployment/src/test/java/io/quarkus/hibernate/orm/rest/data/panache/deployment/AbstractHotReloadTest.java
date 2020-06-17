package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.CollectionsController;
import io.quarkus.test.QuarkusDevModeTest;

public abstract class AbstractHotReloadTest {

    protected abstract QuarkusDevModeTest getTestArchive();

    @Test
    public void shouldModifyPathAndDisableHal() {
        getTestArchive().modifySourceFile(CollectionsController.class,
                s -> s.replace("@ResourceProperties(hal = true, paged = false)", "@ResourceProperties(path = \"col\")"));
        given().accept("application/json")
                .when().get("/col")
                .then().statusCode(200);
        given().accept("application/hal+json")
                .when().get("/col")
                .then().statusCode(406);
    }
}
