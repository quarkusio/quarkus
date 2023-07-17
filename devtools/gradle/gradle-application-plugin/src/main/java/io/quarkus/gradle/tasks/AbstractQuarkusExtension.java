package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusGradleUtils.getSourceSet;
import static java.util.Collections.emptyList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.process.JavaForkOptions;

import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * This base class exists to hide internal properties, make those only available in the {@link io.quarkus.gradle.tasks}
 * package and to the {@link io.quarkus.gradle.extension.QuarkusPluginExtension} class itself.
 */
public abstract class AbstractQuarkusExtension {
    private static final String MANIFEST_SECTIONS_PROPERTY_PREFIX = "quarkus.package.manifest.manifest-sections";
    private static final String MANIFEST_ATTRIBUTES_PROPERTY_PREFIX = "quarkus.package.manifest.attributes";

    private static final String QUARKUS_PROFILE = "quarkus.profile";
    protected final Project project;
    protected final File projectDir;
    protected final Property<String> finalName;
    private final MapProperty<String, String> forcedPropertiesProperty;
    protected final MapProperty<String, String> quarkusBuildProperties;
    private final ListProperty<String> ignoredEntries;
    private final FileCollection classpath;
    private final Property<BaseConfig> baseConfig;
    protected final List<Action<? super JavaForkOptions>> codeGenForkOptions;
    protected final List<Action<? super JavaForkOptions>> buildForkOptions;

    protected AbstractQuarkusExtension(Project project) {
        this.project = project;
        this.projectDir = project.getProjectDir();
        this.finalName = project.getObjects().property(String.class);
        this.finalName.convention(project.provider(() -> String.format("%s-%s", project.getName(), project.getVersion())));
        this.forcedPropertiesProperty = project.getObjects().mapProperty(String.class, String.class);
        this.quarkusBuildProperties = project.getObjects().mapProperty(String.class, String.class);
        this.ignoredEntries = project.getObjects().listProperty(String.class);
        this.ignoredEntries.convention(
                project.provider(() -> baseConfig().packageConfig().userConfiguredIgnoredEntries.orElse(emptyList())));
        this.baseConfig = project.getObjects().property(BaseConfig.class).value(project.provider(this::buildBaseConfig));
        SourceSet mainSourceSet = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME);
        this.classpath = dependencyClasspath(mainSourceSet);
        this.codeGenForkOptions = new ArrayList<>();
        this.buildForkOptions = new ArrayList<>();
    }

    private BaseConfig buildBaseConfig() {
        // Using common code to construct the "base config", which is all the configuration (system properties,
        // environment, application.properties/yaml/yml, project properties) that is available in a Gradle task's
        // _configuration phase_.
        EffectiveConfig effectiveConfig = buildEffectiveConfiguration(Collections.emptyMap());
        return new BaseConfig(effectiveConfig);
    }

    protected BaseConfig baseConfig() {
        this.baseConfig.finalizeValue();
        return this.baseConfig.get();
    }

    protected MapProperty<String, String> forcedPropertiesProperty() {
        return forcedPropertiesProperty;
    }

    protected ListProperty<String> ignoredEntriesProperty() {
        return ignoredEntries;
    }

    protected FileCollection classpath() {
        return classpath;
    }

    protected Manifest manifest() {
        return baseConfig().manifest();
    }

    protected EffectiveConfig buildEffectiveConfiguration(ResolvedDependency appArtifact) {
        Map<String, Object> properties = new HashMap<>();

        exportCustomManifestProperties(properties);

        String userIgnoredEntries = String.join(",", ignoredEntries.get());
        if (!userIgnoredEntries.isEmpty()) {
            properties.put("quarkus.package.user-configured-ignored-entries", userIgnoredEntries);
        }

        properties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        properties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        return buildEffectiveConfiguration(properties);
    }

    private EffectiveConfig buildEffectiveConfiguration(Map<String, Object> properties) {
        Set<File> resourcesDirs = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSourceDirectories()
                .getFiles();

        // Used to handle the (deprecated) buildNative and testNative tasks.
        project.getExtensions().getExtraProperties().getProperties().forEach((k, v) -> {
            if (k.startsWith("quarkus.")) {
                forcedPropertiesProperty.put(k, v.toString());
            }
        });

        return EffectiveConfig.builder()
                .withForcedProperties(forcedPropertiesProperty.get())
                .withTaskProperties(properties)
                .withBuildProperties(quarkusBuildProperties.get())
                .withProjectProperties(project.getProperties())
                .withSourceDirectories(resourcesDirs)
                .withProfile(quarkusProfile())
                .build();
    }

    private String quarkusProfile() {
        String profile = System.getProperty(QUARKUS_PROFILE);
        if (profile == null) {
            profile = System.getenv("QUARKUS_PROFILE");
        }
        if (profile == null) {
            profile = quarkusBuildProperties.get().get(QUARKUS_PROFILE);
        }
        if (profile == null) {
            Object p = project.getProperties().get(QUARKUS_PROFILE);
            if (p != null) {
                profile = p.toString();
            }
        }
        if (profile == null) {
            profile = "prod";
        }
        return profile;
    }

    private static FileCollection dependencyClasspath(SourceSet mainSourceSet) {
        return mainSourceSet.getCompileClasspath().plus(mainSourceSet.getRuntimeClasspath())
                .plus(mainSourceSet.getAnnotationProcessorPath())
                .plus(mainSourceSet.getResources());
    }

    private void exportCustomManifestProperties(Map<String, Object> properties) {
        for (Map.Entry<String, Object> attribute : baseConfig().manifest().getAttributes().entrySet()) {
            properties.put(toManifestAttributeKey(attribute.getKey()),
                    attribute.getValue());
        }

        for (Map.Entry<String, Attributes> section : baseConfig().manifest().getSections().entrySet()) {
            for (Map.Entry<String, Object> attribute : section.getValue().entrySet()) {
                properties
                        .put(toManifestSectionAttributeKey(section.getKey(), attribute.getKey()), attribute.getValue());
            }
        }
    }

    private String toManifestAttributeKey(String key) {
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\"", MANIFEST_ATTRIBUTES_PROPERTY_PREFIX, key);
    }

    private String toManifestSectionAttributeKey(String section, String key) {
        if (section.contains("\"")) {
            throw new GradleException("Manifest section name " + section + " is invalid. \" characters are not allowed.");
        }
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\".\"%s\"", MANIFEST_SECTIONS_PROPERTY_PREFIX, section,
                key);
    }
}
