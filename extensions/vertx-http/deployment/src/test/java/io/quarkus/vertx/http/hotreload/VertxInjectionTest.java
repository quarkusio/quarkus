package io.quarkus.vertx.http.hotreload;

import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class VertxInjectionTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(VertxEventBusConsumer.class, VertxEventBusProducer.class));

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
