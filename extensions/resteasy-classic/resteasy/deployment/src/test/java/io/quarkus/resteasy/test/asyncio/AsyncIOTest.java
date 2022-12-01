package io.quarkus.resteasy.test.asyncio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AsyncIOTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AsyncIOResource.class))
            .withConfigurationResource("application-asyncio.properties");

    @Test
    public void testAsyncIODoesNotBlock() {
        Assertions.assertEquals("OK", RestAssured.get("/asyncio").asString());
    }
}
