package io.quarkus.gradle.tasks;

import static java.lang.String.format;
import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.smallrye.config.SmallRyeConfig;

/**
 * Just show the effective configuration and settings.
 */
public abstract class QuarkusShowEffectiveConfig extends QuarkusBuildTask {

    private final Property<Boolean> saveConfigProperties;

    @Inject
    public QuarkusShowEffectiveConfig() {
        super("Collect dependencies for the Quarkus application, prefer the 'quarkusBuild' task", true);
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
            ApplicationModel appModel = resolveAppModelForBuild();
            EffectiveConfig effectiveConfig = effectiveProvider()
                    .buildEffectiveConfiguration(appModel,
                            getAdditionalForcedProperties().get().getProperties());
            SmallRyeConfig config = effectiveConfig.getConfig();
            List<String> sourceNames = new ArrayList<>();
            config.getConfigSources().forEach(configSource -> sourceNames.add(configSource.getName()));

            Map<String, String> values = new HashMap<>();
            for (String key : config.getMapKeys("quarkus").values()) {
                values.put(key, config.getConfigValue(key).getValue());
            }

            String quarkusConfig = values
                    .entrySet()
                    .stream()
                    .map(e -> format("%s=%s", e.getKey(), e.getValue())).sorted()
                    .collect(Collectors.joining("\n    ", "\n    ", "\n"));
            getLogger().lifecycle("Effective Quarkus configuration options: {}", quarkusConfig);

            String finalName = getExtensionView().getFinalName().get();
            String jarType = config.getOptionalValue("quarkus.package.jar.type", String.class).orElse("fast-jar");
            File fastJar = fastJar();
            getLogger().lifecycle("""
                    Quarkus JAR type:              {}
                    Final name:                    {}
                    Output directory:              {}
                    Fast jar directory (if built): {}
                    Runner jar (if built):         {}
                    Native runner (if built):      {}
                    application.(properties|yaml|yml) sources: {}""",
                    jarType,
                    finalName,
                    outputDirectory(),
                    fastJar,
                    runnerJar(),
                    nativeRunner(),
                    sourceNames.stream().collect(Collectors.joining("\n        ", "\n        ", "\n")));

            if (getSaveConfigProperties().get()) {
                Properties props = new Properties();
                props.putAll(effectiveConfig.getValues());
                Path file = buildDir.toPath().resolve(finalName + ".quarkus-build.properties");
                try (BufferedWriter writer = newBufferedWriter(file)) {
                    props.store(writer, format("Quarkus build properties with JAR type %s", jarType));
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
