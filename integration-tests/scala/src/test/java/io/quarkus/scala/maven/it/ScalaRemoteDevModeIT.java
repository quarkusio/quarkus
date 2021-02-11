package io.quarkus.scala.maven.it;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.it.RunAndCheckWithAgentMojoTestBase;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class ScalaRemoteDevModeIT extends RunAndCheckWithAgentMojoTestBase {

    @Test
    public void testThatTheApplicationIsReloadedOnScalaChange()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/classic-scala", "projects/project-classic-run-scala-change-remote");
        agentDir = initProject("projects/classic-scala", "projects/project-classic-run-scala-change-local");
        runAndCheck();

        // Edit the "Hello" message.
        File source = new File(agentDir, "src/main/scala/org/acme/HelloResource.scala");
        String uuid = UUID.randomUUID().toString();
        filter(source, ImmutableMap.of("= \"hello\"", "= \"" + uuid + "\""));

        // Wait until we get "uuid"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains(uuid));

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        filter(source, ImmutableMap.of(uuid, "carambar"));

        // Wait until we get "carambar"
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES).until(() -> DevModeTestUtils.getHttpResponse("/app/hello").contains("carambar"));
    }
}
