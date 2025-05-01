package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Tests to ensure Quarkus tests behave well with mvn verify
 */
@DisableForNative
class VerifyIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    void testTestsInJar() throws Exception {
        {
            // Prepare the jar containing the tests
            File packagerDir = initProject("projects/tests-in-jar-packager", "projects/tests-in-jar-packager-processed");
            RunningInvoker packagerInvoker = new RunningInvoker(packagerDir, false);
            // We can't run the tests at this stage, since there's no application for them to run against
            MavenProcessInvocationResult result = packagerInvoker
                    .execute(List.of("clean", "install", "-DskipTests"), Map.of());
            assertThat(result.getProcess()
                    .waitFor()).isZero();
        }

        testDir = initProject("projects/tests-in-jar", "projects/tests-in-jar-processed");
        running = new RunningInvoker(testDir, false);

        MavenProcessInvocationResult result = running
                .execute(List.of("clean", "verify"), Map.of());
        assertThat(result.getProcess().waitFor()).isZero();
        // Hardcode a check to make sure some tests ran
        String log = running.log();
        assertThat(log).contains("[INFO] Running org.acme.HelloResourceFromJarTest");
        assertThat(log).contains("[INFO] Running org.acme.ThirdpartyResourceFromJarTest");
        assertThat(log).contains("Tests run: 2, Failures: 0, Errors: 0, Skipped: 0");
    }

}
