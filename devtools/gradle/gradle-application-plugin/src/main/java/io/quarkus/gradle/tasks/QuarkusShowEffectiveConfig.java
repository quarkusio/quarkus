package io.quarkus.gradle.tasks;

import static java.lang.String.format;
import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

/**
 * Just show the effective configuration and settings.
 */
public abstract class QuarkusShowEffectiveConfig extends QuarkusBuildTask {

    private final Property<Boolean> saveConfigProperties;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public QuarkusShowEffectiveConfig(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Collect dependencies for the Quarkus application, prefer the 'quarkusBuild' task");

        this.saveConfigProperties = getProject().getObjects().property(Boolean.class).convention(Boolean.FALSE);
    }

    @Option(option = "save-config-properties", description = "Save the effective Quarkus configuration properties to a file.")
    @Internal
    public Property<Boolean> getSaveConfigProperties() {
        return saveConfigProperties;
    }

    @TaskAction
    public void dumpEffectiveConfiguration() {
        try {
            QuarkusBuildConfiguration effective = effectiveConfig().effective();

            String config = effective.configMap().entrySet().stream()
                    .map(e -> format("%s=%s", e.getKey(), e.getValue())).sorted()
                    .collect(Collectors.joining("\n    ", "\n    ", "\n"));

            getLogger().lifecycle("Effective Quarkus configuration options: {}", config);

            String finalName = effective.extension.finalName();
            String packageType = effective.packageType();
            File fastJar = effective.fastJar();
            getLogger().lifecycle(
                    "" +
                            "Quarkus package type:          {}\n" +
                            "Final name:                    {}\n" +
                            "Output directory:              {}\n" +
                            "Fast jar directory (if built): {}\n" +
                            "Runner jar (if built):         {}\n" +
                            "Native runner (if built):      {}\n" +
                            "application.(properties|yaml|yml) sources: {}",
                    packageType,
                    finalName,
                    effective.outputDirectory(),
                    fastJar,
                    effective.runnerJar(),
                    effective.nativeRunner(),
                    effective.applicationPropsSources().stream().map(Object::toString)
                            .collect(Collectors.joining("\n        ", "\n        ", "\n")));

            if (saveConfigProperties.get()) {
                Properties props = new Properties();
                props.putAll(effective.configMap());
                Path file = getProject().getBuildDir().toPath().resolve(finalName + ".quarkus-build.properties");
                try (BufferedWriter writer = newBufferedWriter(file)) {
                    props.store(writer, format("Quarkus build properties with package type %s", packageType));
                } catch (IOException e) {
                    throw new GradleException("Failed to write Quarkus build configuration settings", e);
                }
                getLogger().lifecycle("\nWrote configuration settings to {}", file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new GradleException("WTF", e);
        }
    }
}
