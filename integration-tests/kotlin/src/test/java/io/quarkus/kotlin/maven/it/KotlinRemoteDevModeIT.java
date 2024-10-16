package io.quarkus.kotlin.maven.it;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.RunAndCheckWithAgentMojoTestBase;

public class KotlinRemoteDevModeIT extends RunAndCheckWithAgentMojoTestBase {

    @Test
    public void testThatTheApplicationIsReloadedOnKotlinChange()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change-remote");
        agentDir = initProject("projects/classic-kotlin", "projects/project-classic-run-kotlin-change-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/kotlin/org/acme/HelloResource.kt");
        String uuid = UUID.randomUUID().toString();
        filter(source, Map.of("return \"hello\"", "return \"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, Map.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.MINUTES).until(() -> devModeClient.getHttpResponse("/app/hello").contains("carambar"));
    }
}
