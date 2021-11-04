package io.quarkus.resteasy.test.asyncio;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AsyncIOUndertowTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AsyncIOResource.class))
            .withConfigurationResource("application-asyncio.properties")
            .setForcedDependencies(
                    Arrays.asList(new AppArtifact("io.quarkus", "quarkus-undertow", Version.getVersion())));

    @Test
    public void testAsyncIODoesNotBlock() {
        Assertions.assertEquals("OK", RestAssured.get("/asyncio").asString());
    }
}
