package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusGradleUtils.getSourceSet;
import static io.smallrye.common.expression.Expression.Flag.DOUBLE_COLON;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;
import static java.util.Collections.emptyList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.expression.Expression;

/**
 * This base class exists to hide internal properties, make those only available in the {@link io.quarkus.gradle.tasks}
 * package and to the {@link io.quarkus.gradle.extension.QuarkusPluginExtension} class itself.
 */
public abstract class AbstractQuarkusExtension {
    private static final String MANIFEST_SECTIONS_PROPERTY_PREFIX = "quarkus.package.jar.manifest.sections";
    private static final String MANIFEST_ATTRIBUTES_PROPERTY_PREFIX = "quarkus.package.jar.manifest.attributes";

    protected static final String QUARKUS_PROFILE = "quarkus.profile";
    protected final Project project;
    protected final File projectDir;
    protected final Property<String> finalName;
    private final MapProperty<String, String> forcedPropertiesProperty;
    protected final MapProperty<String, String> quarkusBuildProperties;
    protected final ListProperty<String> cachingRelevantProperties;
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
        this.cachingRelevantProperties = project.getObjects().listProperty(String.class)
                .value(List.of("quarkus[.].*", "platform[.]quarkus[.].*"));
        this.ignoredEntries = project.getObjects().listProperty(String.class);
        this.ignoredEntries.convention(
                project.provider(() -> baseConfig().packageConfig().jar().userConfiguredIgnoredEntries().orElse(emptyList())));
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
        Set<File> resourcesDirs = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSourceDirectories()
                .getFiles();

        // Used to handle the (deprecated) buildNative and testNative tasks.
        project.getExtensions().getExtraProperties().getProperties().forEach((k, v) -> {
            if (k.startsWith("quarkus.") || k.startsWith("platform.quarkus.")) {
                forcedPropertiesProperty.put(k, v.toString());
            }
        });

        EffectiveConfig effectiveConfig = EffectiveConfig.builder()
                .withForcedProperties(forcedPropertiesProperty.get())
                .withTaskProperties(Collections.emptyMap())
                .withBuildProperties(quarkusBuildProperties.get())
                .withProjectProperties(project.getProperties())
                .withSourceDirectories(resourcesDirs)
                .withProfile(quarkusProfile())
                .build();
        return new BaseConfig(effectiveConfig);
    }

    public BaseConfig baseConfig() {
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

    public Manifest manifest() {
        return baseConfig().manifest();
    }

    public Map<String, Attributes> getAttributes() {
        return manifest().getSections();
    }

    public PackageConfig packageConfig() {
        return baseConfig().packageConfig();
    }

    public Map<String, String> cachingRelevantProperties(List<String> propertyPatterns) {
        return baseConfig().cachingRelevantProperties(propertyPatterns);
    }

    public NativeConfig nativeConfig() {
        return baseConfig().nativeConfig();
    }

    protected EffectiveConfig buildEffectiveConfiguration(ApplicationModel appModel) {
        ResolvedDependency appArtifact = appModel.getAppArtifact();

        Map<String, Object> properties = new HashMap<>();
        exportCustomManifestProperties(properties);

        Set<File> resourcesDirs = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSourceDirectories()
                .getFiles();

        // Used to handle the (deprecated) buildNative and testNative tasks.
        project.getExtensions().getExtraProperties().getProperties().forEach((k, v) -> {
            if (k.startsWith("quarkus.") || k.startsWith("platform.quarkus.")) {
                forcedPropertiesProperty.put(k, v.toString());
            }
        });

        Map<String, String> defaultProperties = new HashMap<>();
        String userIgnoredEntries = String.join(",", ignoredEntries.get());
        if (!userIgnoredEntries.isEmpty()) {
            defaultProperties.put("quarkus.package.jar.user-configured-ignored-entries", userIgnoredEntries);
        }
        defaultProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        defaultProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        return EffectiveConfig.builder()
                .withPlatformProperties(appModel.getPlatformProperties())
                .withForcedProperties(forcedPropertiesProperty.get())
                .withTaskProperties(properties)
                .withBuildProperties(quarkusBuildProperties.get())
                .withProjectProperties(project.getProperties())
                .withDefaultProperties(defaultProperties)
                .withSourceDirectories(resourcesDirs)
                .withProfile(quarkusProfile())
                .build();
    }

    /**
     * Filters resolved Gradle configuration for properties in the Quarkus namespace
     * (as in start with <code>quarkus.</code>). This avoids exposing configuration that may contain secrets or
     * passwords not related to Quarkus (for instance environment variables storing sensitive data for other systems).
     *
     * @param appArtifact the application dependency to retrive the quarkus application name and version.
     * @return a filtered view of the configuration only with <code>quarkus.</code> names.
     */
    protected Map<String, String> buildSystemProperties(ResolvedDependency appArtifact, Map<String, String> quarkusProperties) {
        Map<String, String> buildSystemProperties = new HashMap<>();
        buildSystemProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        buildSystemProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        for (Map.Entry<String, String> entry : forcedPropertiesProperty.get().entrySet()) {
            if (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus.")) {
                buildSystemProperties.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : quarkusBuildProperties.get().entrySet()) {
            if (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus.")) {
                buildSystemProperties.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if ((entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus."))
                    && entry.getValue() != null) {
                buildSystemProperties.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Set<String> quarkusValues = new HashSet<>();
        quarkusValues.addAll(quarkusProperties.values());
        quarkusValues.addAll(buildSystemProperties.values());

        for (String value : quarkusValues) {
            Expression expression = Expression.compile(value, LENIENT_SYNTAX, NO_TRIM, NO_SMART_BRACES, DOUBLE_COLON);
            for (String reference : expression.getReferencedStrings()) {
                String expanded = forcedPropertiesProperty.get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                    continue;
                }

                expanded = quarkusBuildProperties.get().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                    continue;
                }

                expanded = (String) project.getProperties().get(reference);
                if (expanded != null) {
                    buildSystemProperties.put(reference, expanded);
                }
            }
        }

        return buildSystemProperties;
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

    protected static String toManifestAttributeKey(String key) {
        if (key.contains("\"")) {
            throw new GradleException("Manifest entry name " + key + " is invalid. \" characters are not allowed.");
        }
        return String.format("%s.\"%s\"", MANIFEST_ATTRIBUTES_PROPERTY_PREFIX, key);
    }

    protected static String toManifestSectionAttributeKey(String section, String key) {
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
