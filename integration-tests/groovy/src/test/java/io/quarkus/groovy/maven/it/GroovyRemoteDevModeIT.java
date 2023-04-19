package io.quarkus.groovy.maven.it;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.RunAndCheckWithAgentMojoTestBase;
import io.quarkus.test.devmode.util.DevModeTestUtils;

class GroovyRemoteDevModeIT extends RunAndCheckWithAgentMojoTestBase {

    @Test
    void testThatTheApplicationIsReloadedOnGroovyChange()
            throws MavenInvocationException, IOException {
        testDir = initProject("projects/classic-groovy", "projects/project-classic-run-groovy-change-remote");
        agentDir = initProject("projects/classic-groovy", "projects/project-classic-run-groovy-change-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/groovy/org/acme/HelloResource.groovy");
        String uuid = UUID.randomUUID().toString();
        filter(source, Map.of("'hello'", "'" + uuid + "'"));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, Map.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("carambar"));
    }
}
