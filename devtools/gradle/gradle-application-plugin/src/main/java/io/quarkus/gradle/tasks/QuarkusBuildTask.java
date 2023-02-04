package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import io.quarkus.gradle.dsl.Manifest;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Base class for the {@link QuarkusBuildDependencies}, {@link QuarkusBuildApp}, {@link QuarkusBuild} tasks
 */
abstract class QuarkusBuildTask extends QuarkusTask {

    static final String NATIVE_PROPERTY_NAMESPACE = "quarkus.native";
    static final String MANIFEST_SECTIONS_PROPERTY_PREFIX = "quarkus.package.manifest.manifest-sections";
    static final String MANIFEST_ATTRIBUTES_PROPERTY_PREFIX = "quarkus.package.manifest.attributes";

    static final String QUARKUS_ARTIFACT_PROPERTIES = "quarkus-artifact.properties";
    final QuarkusBuildConfiguration buildConfiguration;

    QuarkusBuildTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(description);
        this.buildConfiguration = buildConfiguration;
    }

    QuarkusBuildConfiguration effectiveConfig() {
        return buildConfiguration.effective();
    }

    @Internal
    public boolean isCachedByDefault() {
        switch (effectiveConfig().packageType()) {
            case "jar":
            case "fast-jar":
            case "legacy-jar":
                return true;
            default:
                return false;
        }
    }

    @Optional
    @Input
    public List<String> getIgnoredEntries() {
        return buildConfiguration.ignoredEntries;
    }

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Optional
    @Input
    public abstract MapProperty<String, String> getForcedProperties();

    @Input
    public Map<String, String> getQuarkusBuildSystemProperties() {
        return effectiveConfig().getQuarkusSystemProperties();
    }

    @Input
    public Map<String, String> getQuarkusBuildEnvProperties() {
        return effectiveConfig().getQuarkusEnvProperties();
    }

    /**
     * Retrieve all {@code quarkus.*} properties, which may be relevant for the Quarkus application build, from
     * (likely) all possible sources.
     */
    protected Properties getBuildSystemProperties(ResolvedDependency appArtifact) {
        final Properties realProperties = new Properties();
        realProperties.putAll(effectiveConfig().configMap());
        realProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        realProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());
        return realProperties;
    }

    @Classpath
    public FileCollection getClasspath() {
        return effectiveConfig().classpath;
    }

    @Internal
    public Manifest getManifest() {
        return effectiveConfig().manifest;
    }

    @Input
    public Map<String, String> getCachingRelevantInput() {
        return effectiveConfig().configMap();
    }
}
