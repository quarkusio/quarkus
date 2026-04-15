package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusGradleUtils.getSourceSet;
import static java.util.Collections.emptyList;

import java.io.File;
import java.util.ArrayList;
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
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.process.JavaForkOptions;

import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.gradle.dsl.Manifest;

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
        // Using a ValueSource to construct the "base config" map. The ValueSource wraps all
        // SmallRyeConfig construction (which internally calls System.getProperties()) in an
        // opaque boundary, so Gradle's configuration cache does not track individual system
        // property accesses as inputs. Only the final result map is compared between builds.
        Set<File> resourcesDirs = getSourceSet(project, SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSourceDirectories()
                .getFiles();

        // Filter project properties to quarkus-relevant ones to avoid tracking all project
        // properties as configuration cache inputs.
        Map<String, String> filteredProjectProperties = getQuarkusRelevantProjectProperties();

        Provider<Map<String, String>> configMapProvider = project.getProviders()
                .of(QuarkusConfigValueSource.class, spec -> {
                    spec.getParameters().getBuildProperties().set(quarkusBuildProperties);
                    spec.getParameters().getProjectProperties().set(filteredProjectProperties);
                    spec.getParameters().getSourceDirectories().set(resourcesDirs);
                    spec.getParameters().getProfile().set(quarkusProfile());
                });

        return new BaseConfig(configMapProvider.get());
    }

    /**
     * Returns only quarkus-relevant project properties, to avoid registering all project
     * properties as configuration cache inputs.
     */
    private Map<String, String> getQuarkusRelevantProjectProperties() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() != null
                    && (entry.getKey().startsWith("quarkus.") || entry.getKey().startsWith("platform.quarkus."))) {
                result.put(entry.getKey(), entry.getValue().toString());
            }
        }
        return result;
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

    private String quarkusProfile() {
        // Use Gradle Provider API for CC-compatible single property lookups
        String profile = project.getProviders().systemProperty(QUARKUS_PROFILE).getOrNull();
        if (profile == null) {
            profile = project.getProviders().environmentVariable("QUARKUS_PROFILE").getOrNull();
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
