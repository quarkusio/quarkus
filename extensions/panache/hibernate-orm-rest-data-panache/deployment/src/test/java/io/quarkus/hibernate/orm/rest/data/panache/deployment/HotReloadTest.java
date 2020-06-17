package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.entity.CollectionsController;
import io.quarkus.test.QuarkusDevModeTest;

public class HotReloadTest {

    @RegisterExtension
    public final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(CollectionsController.class.getPackage()) // entity package
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void shouldModifyPathAndDisableHal() {
        TEST.modifySourceFile(CollectionsController.class,
                s -> s.replace("@ResourceProperties(hal = true, paged = false)", "@ResourceProperties(path = \"col\")"));
        given().accept("application/json")
                .when().get("/col")
                .then().statusCode(200);
        given().accept("application/hal+json")
                .when().get("/col")
                .then().statusCode(406);
    }
}
