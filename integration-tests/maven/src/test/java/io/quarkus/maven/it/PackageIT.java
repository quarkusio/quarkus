package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;
import io.quarkus.runtime.util.HashUtil;

@DisableForNative
public class PackageIT extends MojoTestBase {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testConfigTracking() throws Exception {
        testDir = initProject("projects/config-tracking");
        running = new RunningInvoker(testDir, false);
        var configDump = new File(new File(testDir, ".quarkus"), "quarkus-prod-config-dump");
        var configCheck = new File(new File(testDir, "target"), "quarkus-prod-config-check");

        // initial build that generates .quarkus/quarkus-prod-config-dump
        var result = running.execute(List.of("clean package -DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(configDump).exists();
        assertThat(configCheck).doesNotExist();

        // rebuild and compare the files
        result = running.execute(List.of("package -DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(configDump).exists();
        assertThat(configCheck).exists();
        assertThat(configDump).hasSameTextualContentAs(configCheck);

        var props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configDump.toPath())) {
            props.load(reader);
        }
        assertThat(props).containsEntry("quarkus.application.name", HashUtil.sha512("code-with-quarkus"));

        assertThat(props).doesNotContainKey("quarkus.platform.group-id");
        for (var name : props.stringPropertyNames()) {
            assertThat(name).doesNotStartWith("quarkus.test.");
        }

        result = running.execute(List.of("package -DskipTests -Dquarkus.package.jar.type=uber-jar"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(running.log())
                .contains("Option quarkus.package.jar.type has changed since the last build from fast-jar to uber-jar");
    }

    @Test
    public void testConfigTrackingCustomFile() throws Exception {
        testDir = initProject("projects/config-tracking-custom-file");
        running = new RunningInvoker(testDir, false);
        var configDump = new File(new File(testDir, ".quarkus"), "quarkus-prod-used-config-options");
        var configCheck = new File(new File(testDir, "target"), "quarkus-prod-config-check");

        // initial build that generates .quarkus/quarkus-prod-config-dump
        var result = running.execute(List.of("clean package -DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(configDump).exists();
        assertThat(configCheck).doesNotExist();

        // rebuild and compare the files
        result = running.execute(List.of("clean package -DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(configDump).exists();
        assertThat(configCheck).exists();
        assertThat(configDump).hasSameTextualContentAs(configCheck);

        var props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(configDump.toPath())) {
            props.load(reader);
        }
        assertThat(props).containsEntry("quarkus.application.name", HashUtil.sha512("code-with-quarkus"));

        assertThat(props).doesNotContainKey("quarkus.platform.group-id");
        for (var name : props.stringPropertyNames()) {
            assertThat(name).doesNotStartWith("quarkus.test.");
        }

        result = running.execute(List.of("package -DskipTests -Dquarkus.package.jar.type=uber-jar"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
        assertThat(running.log())
                .contains("Option quarkus.package.jar.type has changed since the last build from fast-jar to uber-jar");
    }

    @Test
    public void testPluginClasspathConfig() throws Exception {
        testDir = initProject("projects/test-plugin-classpath-config");
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }

    @Test
    public void testExtensionRemovedResources() throws Exception {
        testDir = initProject("projects/extension-removed-resources");
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(List.of("verify"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }

    @Test
    public void testExtensionTestWithNoMain() throws Exception {
        testDir = initProject("projects/extension-test-with-no-main");
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(List.of("verify"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);
    }

    @Test
    public void testUberJarMavenPluginConfiguration()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/uberjar-maven-plugin-config");
        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(List.of("install", "-DskipTests"), Map.of());
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Path localRunner = getTargetDir().toPath().resolve("acme-quarkus-runner.jar");
        assertThat(localRunner).exists();

        // make sure the runner jar was attached, there was a bug of the runner jar not being attached when the
        // finalName option in the Quarkus plugin didn't match the finalName ocnfigured in the POM's build section
        final Path installedRunner = running.getLocalRepositoryDirectory().toPath().resolve("org").resolve("acme")
                .resolve("acme")
                .resolve("1.0-SNAPSHOT").resolve("acme-1.0-SNAPSHOT-runner.jar");
        assertThat(installedRunner).exists();

        verifyUberJar();
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
    public void testQuarkusPackageOutputDirectory()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/quarkus.package.output-directory");

        running = new RunningInvoker(testDir, false);
        // we do want to run the tests too
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        File targetDir = getTargetDir();
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);

        targetDir = new File(targetDir, "custom-output-dir");
        assertThat(targetDir).exists();
        jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);
    }

    /**
     * POM files are often found among the project's dependencies.
     * This test makes sure such projects can be built with mutable-jar format
     * without choking on non-jar dependencies.
     */
    @Test
    public void testDependencyOnPomMutableJar()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/dependency-on-pom");

        running = new RunningInvoker(testDir, false);
        // we do want to run the tests too
        final MavenProcessInvocationResult result = running.execute(List.of("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        File targetDir = getTargetDir();
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);
    }

    @Test
    public void testPackageWorksWhenUberjarIsTrue()
            throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject("projects/uberjar-check");

        createAndVerifyUberJar();
        // ensure that subsequent package without clean also works
        createAndVerifyUberJar();
    }

    private void createAndVerifyUberJar() throws IOException, MavenInvocationException, InterruptedException {
        Properties p = new Properties();
        p.setProperty("quarkus.package.jar.type", "uber-jar");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(List.of("package"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        verifyUberJar();
    }

    private void verifyUberJar() throws IOException {
        final File targetDir = getTargetDir();
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);
        assertThat(getNumberOfFilesEndingWith(targetDir, ".original")).isEqualTo(1);
        File uberJar = jars.get(0);
        assertMultiReleaseJar(uberJar);
        ensureManifestOfJarIsReadableByJarInputStream(uberJar);
    }

    protected void assertMultiReleaseJar(File uberJar) throws IOException {
        try (JarFile jarFile = new JarFile(uberJar)) {
            // we expect this uber jar to be a multi-release jar since one of its
            // dependencies (smallrye-classloader artifact), from which we composed this uber-jar,
            // is a multi-release jar
            Assertions.assertTrue(jarFile.isMultiRelease(), "uber-jar " + uberJar
                    + " was expected to be a multi-release jar but wasn't");
        }
    }

    @Test
    public void testUberJarWithoutRunnerSuffix()
            throws Exception {
        testDir = initProject("projects/uberjar-check", "projects/uberjar-runner-suffix-off");

        Properties p = new Properties();
        p.setProperty("quarkus.package.jar.type", "uber-jar");
        p.setProperty("quarkus.package.jar.add-runner-suffix", "false");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(List.of("-DskipTests", "package"),
                Map.of(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        List<File> jars = getFilesEndingWith(targetDir, ".jar");
        assertThat(jars).hasSize(1);
        File jarFile = jars.get(0);
        assertThat(jarFile.getName()).isEqualTo("acme-1.0-SNAPSHOT.jar");

        assertMultiReleaseJar(jarFile);
        ensureManifestOfJarIsReadableByJarInputStream(jarFile);
    }

    @Test
    public void testCustomPackaging()
            throws Exception {
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

        final Path runnerJar = getTargetDir().toPath().resolve("quarkus-app").resolve("quarkus-run.jar");
        Assertions.assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
        assertZipEntriesCanBeOpenedAndClosed(runnerJar);
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
        testDir = initProject("projects/uberjar-check", "projects/project-uberjar-crc");

        running = new RunningInvoker(testDir, false);

        Properties p = new Properties();
        p.setProperty("quarkus.package.jar.type", "uber-jar");
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap(), p);
        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        assertThat(getNumberOfFilesEndingWith(targetDir, ".jar")).isEqualTo(1);
        assertThat(getNumberOfFilesEndingWith(targetDir, ".original")).isEqualTo(1);

        final Path runnerJar = targetDir.toPath().resolve("acme-1.0-SNAPSHOT-runner.jar");
        Assertions.assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
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
    public void testLegacyJarHasValidCRC() throws Exception {
        testDir = initProject("projects/uberjar-check", "projects/project-legacyjar-crc");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.singletonMap("QUARKUS_PACKAGE_JAR_TYPE", "legacy-jar"));

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();
        assertThat(getNumberOfFilesEndingWith(targetDir, ".jar")).isEqualTo(2);

        final Path runnerJar = targetDir.toPath().resolve("acme-1.0-SNAPSHOT-runner.jar");
        Assertions.assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
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
    public void testFastJarHasValidCRC() throws Exception {
        testDir = initProject("projects/uberjar-check", "projects/project-fastjar-crc");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final Path runnerJar = getTargetDir().toPath().resolve("quarkus-app").resolve("quarkus-run.jar");
        Assertions.assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
        assertZipEntriesCanBeOpenedAndClosed(runnerJar);
    }

    /**
     * Tests that quarkus.index-dependency.* can be used for modules in a multimodule project
     */
    @Test
    public void testQuarkusIndexDependencyOnLocalModule() throws Exception {
        testDir = initProject("projects/quarkus-index-dependencies");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = new File(testDir.getAbsoluteFile(), "runner" + File.separator + "target");

        final Path runnerJar = targetDir.toPath().resolve("quarkus-app").resolve("quarkus-run.jar");
        Assertions.assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
        assertZipEntriesCanBeOpenedAndClosed(runnerJar);
    }

    /**
     * Tests that quarkus.index-dependency.* can be used for modules in a multimodule project. artifact-id is optional.
     */
    @Test
    public void testQuarkusIndexDependencyGroupIdOnLocalModule() throws Exception {
        testDir = initProject("projects/quarkus-index-dependencies-groupid");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = new File(testDir.getAbsoluteFile(), "runner" + File.separator + "target");

        final Path runnerJar = targetDir.toPath().resolve("quarkus-app").resolve("quarkus-run.jar");
        Assertions.assertTrue(Files.exists(runnerJar), "Runner jar " + runnerJar + " is missing");
        assertZipEntriesCanBeOpenedAndClosed(runnerJar);
    }

    @Test
    public void testNativeSourcesPackage() throws Exception {
        testDir = initProject("projects/uberjar-check", "projects/project-native-sources");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(
                Arrays.asList("package", "-Dquarkus.native.enabled=true", "-Dquarkus.native.sources-only=true"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        final File targetDir = getTargetDir();

        final Path nativeSourcesDir = targetDir.toPath().resolve("native-sources");
        assertThat(nativeSourcesDir).exists()
                .isDirectoryContaining(p -> "native-image.args".equals(p.getFileName().toString()))
                .isDirectoryContaining(p -> "acme-1.0-SNAPSHOT-runner.jar".equals(p.getFileName().toString()));

    }

    @Test
    public void testMultiJarModulesPackage() throws Exception {
        testDir = initProject("projects/multijar-module", "projects/multijar-module-package");

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(
                Arrays.asList("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);
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
