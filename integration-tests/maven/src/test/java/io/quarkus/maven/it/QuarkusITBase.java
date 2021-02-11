package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

// meant to test that @QuarkusIntegrationTest can properly launch jars
abstract class QuarkusITBase extends MojoTestBase {

    void doTest(String projectName, String profile) throws MavenInvocationException, IOException {
        File testDir = initProject("projects/reactive-routes", "projects/" + projectName);
        RunningInvoker packageInvocation = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult packageInvocationResult = packageInvocation
                .execute(Arrays.asList("package", "-B",
                        "-D" + profile), Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES)
                .until(() -> packageInvocationResult.getProcess() != null && !packageInvocationResult.getProcess().isAlive());
        assertThat(packageInvocation.log()).containsIgnoringCase("BUILD SUCCESS");

        RunningInvoker integrationTestsInvocation = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult integrationTestsInvocationResult = integrationTestsInvocation
                .execute(Arrays.asList("failsafe:integration-test", "-B",
                        "-D" + profile), Collections.emptyMap());

        await().atMost(1, TimeUnit.MINUTES)
                .until(() -> integrationTestsInvocationResult.getProcess() != null
                        && !integrationTestsInvocationResult.getProcess().isAlive());
        assertThat(integrationTestsInvocation.log()).containsIgnoringCase("BUILD SUCCESS");
    }
}
