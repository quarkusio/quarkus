package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

@DisableForNative
public class PackageIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testPackageWorksWhenUberjarIsFalse()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-false");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.singletonMap("QUARKUS_PACKAGE_UBER_JAR", "false"));

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(2);

        // make sure the jar can be read by JarInputStream
        ensureManifestOfJarIsReadableByJarInputStream(
                jars.stream().filter(f -> f.getName().contains("-runner")).findFirst().get());
    }

    private void ensureManifestOfJarIsReadableByJarInputStream(File jar) throws IOException {
        try (InputStream fileInputStream = new FileInputStream(jar)) {
            try (JarInputStream stream = new JarInputStream(fileInputStream)) {
                Manifest manifest = stream.getManifest();
                assertThat(manifest).isNotNull();
            }
        }
    }

    @Test
    public void testPackageWorksWhenUberjarIsTrue()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-true");

        createAndVerifyUberJar();
        // ensure that subsequent package without clean also works
        createAndVerifyUberJar();
    }

    private File createAndVerifyUberJar() throws IOException, MavenInvocationException, InterruptedException {
        Properties p = new Properties();
        p.setProperty("quarkus.package.uber-jar", "true");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);
        final File uberJar = jars.get(0);
        assertThat(getNumberOfFilesEndingWith(targetDir, ".original")).isEqualTo(1);

        ensureManifestOfJarIsReadableByJarInputStream(uberJar);
        return uberJar;
    }

    @Test
    public void testCustomPackaging()
            throws MavenInvocationException, FileNotFoundException, InterruptedException {
        testDir = getTargetDir("projects/custom-packaging-plugin");

        running = new RunningInvoker(testDir, false);
        MavenProcessInvocationResult result = running.execute(Collections.singletonList("install"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        testDir = getTargetDir("projects/custom-packaging-app");

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
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-true2");

        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.package.uber-jar", "true");
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap(), p);
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
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-false2");

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

    /**
     * Test that if an application, being built as a uber jar, contains a certain file and the application's
     * dependency too has the same file, then the uber jar will always contain the application's file and not
     * the one from its dependencies.
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/6754"/>
     */
    @Test
    public void testDeterminsticResourceInclusionUberJar() throws Exception {
        // first "mvn install" the dependency project used by the uberjar-check
        final File dependencyProjectDir = initProject("projects/uberjar-dependency", "projects/project-uberjar-dependency");
        final RunningInvoker invoker = new RunningInvoker(dependencyProjectDir, false);
        final MavenProcessInvocationResult result = invoker.execute(Collections.singletonList("clean install"),
                Collections.emptyMap());
        assertEquals(0, result.getProcess().waitFor(), "uberjar-dependency project build failed");

        // now build the application, which depends on the previous project, as a uber jar
        testDir = initProject("projects/uberjar-resource-inclusion", "projects/project-uberjar-resource-inclusion");
        final File uberJar = createAndVerifyUberJar();
        // verify that any duplicate resource from within dependencies of the application
        // doesn't get included instead of the one from the application itself
        try (final JarFile jar = new JarFile(uberJar)) {
            final JarEntry jarEntry = jar.getJarEntry("application.properties");
            assertNotNull(jarEntry, "application.properties is missing in uber jar");
            try (final InputStream is = jar.getInputStream(jarEntry)) {
                final Properties props = new Properties();
                props.load(is);
                assertEquals("from uberjar-resource-inclusion", props.get("which-application-properties"),
                        "Unexpected application.properties included in uber jar");
            }
            final String someOtherResource = "META-INF/resources/helloworld.properties";
            final JarEntry someOtherResourceJarEntry = jar.getJarEntry(someOtherResource);
            assertNotNull(someOtherResourceJarEntry, someOtherResource + " is missing in uber jar");
            try (final InputStream is = jar.getInputStream(someOtherResourceJarEntry)) {
                final Properties props = new Properties();
                props.load(is);
                assertEquals("hello from uberjar-resource-inclusion", props.get("greeting"),
                        "Unexpected " + someOtherResource + " included in uber jar");
            }
        }

    }

    private List<File> getFilesEndingWith(File dir, String suffix) {
        final File[] files = dir.listFiles((d, name) -> name.endsWith(suffix));
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    private int getNumberOfFilesEndingWith(File dir, String suffix) {
        return getFilesEndingWith(dir, suffix).size();
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
