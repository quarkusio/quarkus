package io.quarkus.it.jpa.entitylistener;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EntityListenerTest {

    @Test
    public void entityListenersAnnotation() {
        when().get("/jpa-test/entity-listener/entity-listeners-annotation").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    public void entityInstanceMethods() {
        when().get("/jpa-test/entity-listener/entity-instance-methods").then()
                .body(is("OK"))
                .statusCode(200);
    }

}
