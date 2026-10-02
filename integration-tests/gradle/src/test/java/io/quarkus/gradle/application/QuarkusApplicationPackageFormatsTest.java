package io.quarkus.gradle.application;

import static io.quarkus.gradle.BuildResult.UPTODATE_OUTCOME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;

class QuarkusApplicationPackageFormatsTest extends QuarkusApplicationGradleTestBase {

    private static final Instant DEFAULT_PACKAGE_OUTPUT_TIMESTAMP = Instant.parse("1970-01-02T00:00:00Z");
    private static final String RESPONSE = "hello from the application plugin";

    @Test
    void allNamedJarFormatsAreExecutable() throws Exception {
        File projectDir = getProjectDir("application-plugin/package-formats");

        BuildResult result = runApplicationGradleWrapper(projectDir,
                "clean",
                "quarkusFastBuild",
                "quarkusMutableBuild",
                "quarkusLegacyBuild",
                "quarkusUberBuild",
                "verifyNamedPackageReceipts",
                "intTest",
                ":consumer:stagePackageVariants");

        assertThat(result.unsuccessfulTasks()).isEmpty();
        assertThat(result.getTasks()).containsKeys(
                ":quarkusFastBuild",
                ":quarkusMutableBuild",
                ":quarkusLegacyBuild",
                ":quarkusUberBuild",
                ":quarkusFastIntegrationTestMetadata",
                ":verifyNamedPackageReceipts",
                ":intTest",
                ":consumer:copyFastPackage",
                ":consumer:copyFastLauncher",
                ":consumer:copyLegacyPackage",
                ":consumer:copyUberLauncher",
                ":consumer:packageZip",
                ":consumer:packageTar",
                ":consumer:publishPackageArchivePublicationToTestRepository",
                ":consumer:recordResolvedLaunchers",
                ":consumer:stagePackageVariants");

        Path buildDirectory = projectDir.toPath().resolve("build");
        String archiveName = "application-package-formats-1.0.0-SNAPSHOT-runner.jar";
        List<PackageArtifact> artifacts = List.of(
                new PackageArtifact("fast", "fast-jar", "quarkus-run.jar", false, false, true),
                new PackageArtifact("mutable", "mutable-jar", "quarkus-run.jar", true, false, true),
                new PackageArtifact("legacy", "legacy-jar", archiveName, false, false, true),
                new PackageArtifact("uber", "uber-jar", archiveName, false, true, false));

        for (PackageArtifact artifact : artifacts) {
            Path outputDirectory = buildDirectory.resolve(Path.of("quarkus-builds", artifact.buildName(), "package"));
            Path jar = outputDirectory.resolve(artifact.jarName());
            assertManifest(jar, artifact.buildName());
            assertPackageReceipt(buildDirectory, artifact);
            assertArchiveTimestamps(jar);
        }
        assertArchiveTimestamps(buildDirectory.resolve(
                "quarkus-builds/fast/package/app/application-package-formats-1.0.0-SNAPSHOT.jar"));
        assertArchiveTimestamps(buildDirectory.resolve(
                "quarkus-builds/mutable/package/app/application-package-formats-1.0.0-SNAPSHOT.jar"));
        assertThat(buildDirectory.resolve("quarkus-builds/fast/package/package-augmentation-result.properties"))
                .doesNotExist();
        assertThat(buildDirectory.resolve("quarkus-build-results/fast/package/package-augmentation-result.properties"))
                .isRegularFile();
        assertThat(buildDirectory.resolve("quarkus-builds/fast/package/quarkus-artifact.properties")).doesNotExist();
        assertThat(buildDirectory.resolve("quarkus-build-results/fast/package/quarkus-artifact.properties"))
                .isRegularFile();
        assertThat(buildDirectory.resolve("quarkus/application-model/quarkus-application-model.dat")).isRegularFile();
        assertThat(buildDirectory.resolve(
                "quarkus/application-model/pom-closure/quarkusApplicationModel.properties")).isRegularFile();
        assertThat(buildDirectory.resolve("verification/named-package-results.txt"))
                .hasContent("""
                        fast
                        mutable
                        legacy
                        uber
                        """);

        Path consumerBuildDirectory = projectDir.toPath().resolve("consumer/build");
        Properties resolvedLaunchers = loadProperties(
                consumerBuildDirectory.resolve("resolved-launchers.properties"));
        Path fastOutput = buildDirectory.resolve("quarkus-builds/fast/package");
        Path legacyOutput = buildDirectory.resolve("quarkus-builds/legacy/package");
        assertThat(Path.of(resolvedLaunchers.getProperty("fast"))).isEqualTo(fastOutput.resolve("quarkus-run.jar"));
        assertThat(Path.of(resolvedLaunchers.getProperty("legacy"))).isEqualTo(legacyOutput.resolve(archiveName));

        assertJarApplication(
                buildDirectory.resolve("quarkus-builds/mutable/package/quarkus-run.jar"),
                buildDirectory.resolve("mutable-producer-output.log"), Map.of("QUARKUS_LAUNCH_DEVMODE", "true"),
                RESPONSE);

        assertRelocatedPackage(fastOutput,
                consumerBuildDirectory.resolve("packages/fast/quarkus-run.jar"),
                buildDirectory.resolve("fast-relocated-output.log"));
        Path copiedFastLauncherDirectory = consumerBuildDirectory.resolve("launchers/fast");
        assertThat(copiedFastLauncherDirectory.resolve("quarkus-run.jar")).isRegularFile();
        try (var copiedLauncherFiles = Files.list(copiedFastLauncherDirectory)) {
            assertThat(copiedLauncherFiles).containsExactly(copiedFastLauncherDirectory.resolve("quarkus-run.jar"));
        }
        assertThat(consumerBuildDirectory.resolve("packages/fast/lib")).isDirectory();
        assertRelocatedPackage(legacyOutput,
                consumerBuildDirectory.resolve("packages/legacy").resolve(archiveName),
                buildDirectory.resolve("legacy-relocated-output.log"));
        assertJarApplication(
                consumerBuildDirectory.resolve("launchers/uber").resolve(archiveName),
                buildDirectory.resolve("uber-copied-output.log"), Map.of(), RESPONSE);
        assertZipContains(
                consumerBuildDirectory.resolve("distributions/consumer-1.0-quarkus-app.zip"),
                "application/quarkus-run.jar");
        assertThat(consumerBuildDirectory.resolve("distributions/consumer-1.0-quarkus-app.tgz")).isRegularFile();
        assertZipContains(
                consumerBuildDirectory
                        .resolve("repository/org/acme/distribution/consumer/1.0/consumer-1.0-quarkus-app.zip"),
                "application/quarkus-run.jar");

        BuildResult cacheWarmupResult = runApplicationGradleWrapper(projectDir,
                "verifyNamedPackageReceipts",
                ":consumer:stagePackageVariants");
        assertThat(cacheWarmupResult.unsuccessfulTasks()).isEmpty();
        assertThat(cacheWarmupResult.getTasks())
                .containsEntry(":quarkusFastBuild", UPTODATE_OUTCOME)
                .containsEntry(":quarkusMutableBuild", UPTODATE_OUTCOME)
                .containsEntry(":quarkusLegacyBuild", UPTODATE_OUTCOME)
                .containsEntry(":quarkusUberBuild", UPTODATE_OUTCOME)
                .containsEntry(":verifyNamedPackageReceipts", UPTODATE_OUTCOME);

        BuildResult cachedResult = runApplicationGradleWrapper(projectDir,
                "verifyNamedPackageReceipts",
                ":consumer:stagePackageVariants");
        assertThat(cachedResult.unsuccessfulTasks()).isEmpty();
        assertThat(cachedResult.getOutput()).contains("Configuration cache entry reused.");
    }

    private void assertRelocatedPackage(Path producerPackage, Path copiedLauncher, Path output)
            throws Exception {
        Path unavailablePackage = producerPackage.resolveSibling(producerPackage.getFileName() + "-unavailable");
        assertThat(unavailablePackage).doesNotExist();
        Files.move(producerPackage, unavailablePackage);
        try {
            assertJarApplication(copiedLauncher, output, Map.of(), RESPONSE);
        } finally {
            Files.move(unavailablePackage, producerPackage);
        }
        assertThat(producerPackage).isDirectory();
    }

    private static Properties loadProperties(Path file) throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    private static void assertZipContains(Path archive, String entry) throws IOException {
        assertThat(archive).isRegularFile();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertThat(zip.getEntry(entry)).as("%s in %s", entry, archive).isNotNull();
        }
    }

    private static void assertManifest(Path jar, String buildName) throws Exception {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            var manifest = jarFile.getManifest();
            assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo(buildName);
            assertThat(manifest.getMainAttributes().getValue("Build-Version")).isEqualTo("1.0");
            assertThat(manifest.getEntries().get("Specification").getValue("Specification-Title"))
                    .isEqualTo(buildName + " specification");
            assertThat(manifest.getEntries().get("Build/Info").getValue("Build-Name")).isEqualTo(buildName);
        }
    }

    private static void assertPackageReceipt(Path buildDirectory, PackageArtifact artifact) throws IOException {
        Path receipt = buildDirectory.resolve(
                Path.of("quarkus-build-results", artifact.buildName(), "package", "package-result.properties"));
        Properties properties = loadProperties(receipt);
        Path outputRoot = receipt.getParent().resolve(properties.getProperty("package.output-root")).normalize();
        assertThat(properties)
                .containsEntry("schema.version", "1")
                .containsEntry("result.type", "jvm-package")
                .containsEntry("build.name", artifact.buildName())
                .containsEntry("package.type", artifact.buildType())
                .containsEntry("package.output-name", "application-package-formats-1.0.0-SNAPSHOT")
                .containsEntry("package.jar.path", artifact.jarName())
                .containsEntry("package.mutable", Boolean.toString(artifact.mutable()))
                .containsEntry("package.uber", Boolean.toString(artifact.uberJar()))
                .doesNotContainKey("package.original-artifact");
        assertThat(outputRoot)
                .isEqualTo(buildDirectory.resolve(Path.of("quarkus-builds", artifact.buildName(), "package")));
        assertThat(outputRoot.resolve(properties.getProperty("package.jar.path"))).isRegularFile();
        if (artifact.hasLibraryDirectory()) {
            assertThat(properties).containsEntry("package.library-dir", "lib");
            assertThat(outputRoot.resolve("lib")).isDirectory();
        } else {
            assertThat(properties).doesNotContainKey("package.library-dir");
        }
    }

    private static void assertArchiveTimestamps(Path archive) throws IOException {
        try (JarFile jar = new JarFile(archive.toFile())) {
            assertThat(jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getLastModifiedTime().toInstant()))
                    .as(archive.toString())
                    .isNotEmpty()
                    .allMatch(DEFAULT_PACKAGE_OUTPUT_TIMESTAMP::equals);
        }
    }

    private record PackageArtifact(
            String buildName,
            String buildType,
            String jarName,
            boolean mutable,
            boolean uberJar,
            boolean hasLibraryDirectory) {
    }
}
