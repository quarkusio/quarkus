package io.quarkus.kotlin.maven.it;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class KotlinDevModeIT extends RunAndCheckMojoTestBase {

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChange() throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change");
        runAndCheck(false);

        // Edit the "Hello" message.
        File jaxRsResource = new File(testDir, "src/main/kotlin/org/acme/HelloResource.kt");
        String uuid = UUID.randomUUID().toString();
        filter(jaxRsResource, ImmutableMap.of("return \"hello\"", "return \"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(jaxRsResource::isFile);

        filter(jaxRsResource, ImmutableMap.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("carambar"));

        File greetingService = new File(testDir, "src/main/kotlin/org/acme/GreetingService.kt");
        String newUuid = UUID.randomUUID().toString();
        filter(greetingService, ImmutableMap.of("\"hello\"", "\"" + newUuid + "\""));

        // Wait until we get "newUuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello/bean").contains(newUuid));
    }

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChangeWithCustomCompilerArgs()
            throws MavenInvocationException, IOException {
        testDir = initProject("projects/kotlin-compiler-args", "projects/kotlin-compiler-args-change");
        runAndCheck(false);

        // Edit the "Hello" message.
        File jaxRsResource = new File(testDir, "src/main/kotlin/org/acme/HelloResource.kt");
        String uuid = UUID.randomUUID().toString();
        filter(jaxRsResource, ImmutableMap.of("\"hello\"", "\"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));
    }
}
