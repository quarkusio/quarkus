package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

public class PackageIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testPackageWorksWhenUberjarIsFalse()
            throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-false");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.singletonMap("QUARKUS_PACKAGE_TYPES", "thin-jar"));

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        assertThat(getNumberOfFilesEndingWith(targetDir, ".jar")).isEqualTo(2);
    }

    @Test
    public void testPackageWorksWhenUberjarIsTrue()
            throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-true");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.singletonMap("QUARKUS_PACKAGE_TYPES", "uber-jar"));
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        assertThat(getNumberOfFilesEndingWith(targetDir, ".jar")).isEqualTo(1);
        assertThat(getNumberOfFilesEndingWith(targetDir, ".original")).isEqualTo(1);
    }

    @Test
    public void testCustomPackaging()
            throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = initProject("projects/custom-packaging-plugin", "projects/custom-packaging-plugin");

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("install"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        testDir = initProject("projects/custom-packaging-app", "projects/custom-packaging-app");

        running = new RunningInvoker(testDir, false);
        result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        final File[] files = targetDir.listFiles(f -> f.getName().endsWith(".jar"));
        Set<String> jarNames = new HashSet<>(files.length);
        for (File f : files) {
            jarNames.add(f.getName());
        }
        assertEquals(new HashSet<>(Arrays.asList(new String[] { "acme-custom-packaging-app-1.0-SNAPSHOT-runner.jar",
                "acme-custom-packaging-app-1.0-SNAPSHOT.jar" })),
                jarNames);
    }

    private int getNumberOfFilesEndingWith(File dir, String suffix) {
        final File[] files = dir.listFiles((d, name) -> name.endsWith(suffix));
        return files != null ? files.length : 0;
    }

    private File getTargetDir() {
        return new File(testDir.getAbsoluteFile() + "/target");
    }
}
