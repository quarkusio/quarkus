package io.quarkus.cache.test.devmode;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class CacheHotReloadTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(CacheHotReloadResource.class));

    @Test
    public void testHotReload() {
        checkInvocations("0");

        checkBody("hello foo!");
        checkInvocations("1");

        checkBody("hello foo!");
        checkInvocations("1");

        TEST.modifySourceFile(CacheHotReloadResource.class.getSimpleName() + ".java", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("hello", "goodbye");
            }
        });

        checkInvocations("0");

        checkBody("goodbye foo!");
        checkInvocations("1");

        checkBody("goodbye foo!");
        checkInvocations("1");
    }

    private void checkInvocations(String expectedBody) {
        RestAssured.when().get("/cache-hot-reload-test/invocations").then().statusCode(200)
                .body(is(expectedBody));
    }

    private void checkBody(String expectedBody) {
        RestAssured.when().get("/cache-hot-reload-test/greet?key=foo").then().statusCode(200)
                .body(is(expectedBody));
    }
}
