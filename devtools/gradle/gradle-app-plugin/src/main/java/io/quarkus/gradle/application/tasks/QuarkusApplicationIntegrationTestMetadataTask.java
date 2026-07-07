
package io.quarkus.gradle.application.tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;

/**
 * Generated implementation task that derives launcher metadata for an integration-test suite from a named JVM package
 * result.
 * <p>
 * Public visibility is required for Gradle decoration. The plugin owns this task's instances, inputs, output, and
 * invocation order; it is not a supported typed user entry point. No compatibility commitment is made for direct
 * construction, additional registration, or subclassing. The metadata is cheap to regenerate and is not build-cached.
 */
@DisableCachingByDefault(because = "Integration-test launch metadata is cheap to regenerate")
public abstract class QuarkusApplicationIntegrationTestMetadataTask extends DefaultTask {

    private static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";
    private static final String PATH = "path";
    private static final String LIBRARY_DIR = "metadata.library-dir";

    /**
     * Returns the named package operation's result receipt.
     *
     * @return the package result file
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getPackageResultFile();

    /**
     * Returns Quarkus's relocated-artifact metadata produced beside the package result.
     *
     * @return the relocated artifact properties file
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getRelocatedArtifactPropertiesFile();

    /**
     * Returns the complete launcher metadata directory owned by this task.
     *
     * @return the launcher metadata directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getLauncherMetadataDirectory();

    /**
     * Validates the package receipt and writes package path and optional library-directory metadata consumed by the test
     * launcher.
     */
    @TaskAction
    public void generateIntegrationTestMetadata() {
        Path packageResultFile = getPackageResultFile().get().getAsFile().toPath();
        Path relocatedArtifactPropertiesFile = getRelocatedArtifactPropertiesFile().get().getAsFile().toPath();
        Path launcherMetadataDirectory = getLauncherMetadataDirectory().get().getAsFile().toPath();

        Properties source = new Properties();
        try (var reader = Files.newBufferedReader(relocatedArtifactPropertiesFile)) {
            source.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read Quarkus artifact metadata " + relocatedArtifactPropertiesFile, e);
        }

        var packageResult = new PackageResultCodec().read(packageResultFile);
        Map<String, String> target = new LinkedHashMap<>();
        source.stringPropertyNames().stream()
                .sorted()
                .forEach(key -> target.put(key, source.getProperty(key)));
        target.put(PATH, packageResult.jarPath().toAbsolutePath().normalize().toString());
        packageResult.libraryDirectory().ifPresent(libraryDirectory -> target.put(
                LIBRARY_DIR, libraryDirectory.toAbsolutePath().normalize().toString()));

        try {
            Files.createDirectories(launcherMetadataDirectory);
            PropertyUtils.store(target, launcherMetadataDirectory.resolve(QUARKUS_ARTIFACT_PROPERTIES));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write Quarkus integration-test launch metadata in " + launcherMetadataDirectory, e);
        }
    }
}
