package org.jboss.shamrock.maven.it;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jboss.shamrock.maven.it.verifier.MavenProcessInvocationResult;
import org.jboss.shamrock.maven.it.verifier.RunningInvoker;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testPackageWorksWhenUberjarIsFalse() throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-false");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"), Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }

    @Test
    public void testPackageWorksWhenUberjarIsTrue() throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-true");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"), new HashMap<String, String>() {{
            put("UBER_JAR", "true");
        }});
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }
}
