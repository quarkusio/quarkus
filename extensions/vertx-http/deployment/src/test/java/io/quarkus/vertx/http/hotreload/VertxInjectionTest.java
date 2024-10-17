package io.quarkus.vertx.http.hotreload;

import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.RegistryClientTestHelper;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class VertxInjectionTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(VertxEventBusConsumer.class, VertxEventBusProducer.class));

    @BeforeAll
    public static void setupTestRegistry() {
        RegistryClientTestHelper.enableRegistryClientTestConfig();
    }

    @AfterAll
    public static void cleanupTestRegistry() {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }

    @Test
    public void testEditingBeanUsingVertx() {
        RestAssured.get("/").then()
                .statusCode(200)
                .body(containsString("hello"));

        TEST.modifySourceFile("VertxEventBusConsumer.java", s -> s.replace("hello", "bonjour"));
        RestAssured.get("/").then()
                .statusCode(200)
                .body(containsString("bonjour"));
    }

}
