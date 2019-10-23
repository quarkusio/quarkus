package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.assertj.core.util.Arrays;
import org.junit.Assert;
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

    /**
     * Tests that the uber runner jar created by Quarkus has valid CRC entries. The verification
     * is pretty trivial and involves opening and closing the ZipEntry entries that are part of the
     * runner jar. That internally triggers the CRC checks.
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/4782"/>
     */
    @Test
    public void testRunnerUberJarHasValidCRC() throws Exception {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-true");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.singletonMap("QUARKUS_PACKAGE_TYPES", "uber-jar"));
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        assertThat(getNumberOfFilesEndingWith(targetDir, ".jar")).isEqualTo(1);
        assertThat(getNumberOfFilesEndingWith(targetDir, ".original")).isEqualTo(1);

        final Path runnerJar = targetDir.toPath().resolve("acme-1.0-SNAPSHOT-runner.jar");
        Assert.assertTrue("Runner jar " + runnerJar + " is missing", Files.exists(runnerJar));
        assertZipEntriesCanBeOpenedAndClosed(runnerJar);
    }

    /**
     * Tests that the runner jar created by Quarkus has valid CRC entries. The verification
     * is pretty trivial and involves opening and closing the ZipEntry entries that are part of the
     * runner jar. That internally triggers the CRC checks.
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/4782"/>
     */
    @Test
    public void testRunnerJarHasValidCRC() throws Exception {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-false");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.singletonMap("QUARKUS_PACKAGE_TYPES", "thin-jar"));

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        assertThat(getNumberOfFilesEndingWith(targetDir, ".jar")).isEqualTo(2);

        final Path runnerJar = targetDir.toPath().resolve("acme-1.0-SNAPSHOT-runner.jar");
        Assert.assertTrue("Runner jar " + runnerJar + " is missing", Files.exists(runnerJar));
        assertZipEntriesCanBeOpenedAndClosed(runnerJar);
    }

    private int getNumberOfFilesEndingWith(File dir, String suffix) {
        final File[] files = dir.listFiles((d, name) -> name.endsWith(suffix));
        return files != null ? files.length : 0;
    }

    private File getTargetDir() {
        return new File(testDir.getAbsoluteFile() + "/target");
    }

    private void assertZipEntriesCanBeOpenedAndClosed(final Path jar) throws Exception {
        try (final InputStream is = Files.newInputStream(jar)) {
            final ZipInputStream zis = new ZipInputStream(is);
            ZipEntry e = null;
            while ((e = zis.getNextEntry()) != null) {
                zis.closeEntry();
            }
        }
    }
}
