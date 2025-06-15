package io.quarkus.hibernate.reactive.context;

import static io.restassured.RestAssured.given;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

/**
 * Checks that Hibernate Reactive will refuse to store a contextual Session into a Vert.x session which has been
 * explicitly disabled by using
 * {@link io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle#setCurrentContextSafe(boolean)}.
 */
public class ContextValidationTest {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest().withApplicationRoot((jar) -> jar
            .addClasses(Fruit.class, ContextFruitResource.class).addAsResource("application.properties").addAsResource(
                    new StringAsset("INSERT INTO context_fruits(id, name) VALUES (1, 'Mango');\n"), "import.sql"));

    @Test
    public void testListAllFruits() {
        // This should work fine:
        given().when().get("/contextTest/valid").then().statusCode(200).contentType("application/json").extract()
                .response();

        // This should throw an exception (status code 500):
        given().when().get("/contextTest/invalid").then().statusCode(500).contentType("application/json").extract()
                .response();
    }
}
