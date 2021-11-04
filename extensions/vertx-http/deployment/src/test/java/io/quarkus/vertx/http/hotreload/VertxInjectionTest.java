package io.quarkus.vertx.http.hotreload;

import static org.hamcrest.CoreMatchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;

public class VertxInjectionTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(VertxEventBusConsumer.class, VertxEventBusProducer.class));

    @TestHTTPResource
    URL url;

    @Test
    public void testEditingBeanUsingVertx() {
        RestAssured.get("http://localhost:" + url.getPort() + "/").then()
                .statusCode(200)
                .body(containsString("hello"));

        TEST.modifySourceFile("VertxEventBusConsumer.java", s -> s.replace("hello", "bonjour"));
        RestAssured.get("http://localhost:" + url.getPort() + "/").then()
                .statusCode(200)
                .body(containsString("bonjour"));
    }

}
