package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.AbstractQuarkusExtension.QUARKUS_PROFILE;
import static io.quarkus.gradle.tasks.AbstractQuarkusExtension.toManifestAttributeKey;
import static io.quarkus.gradle.tasks.AbstractQuarkusExtension.toManifestSectionAttributeKey;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ResolvedDependency;

public class EffectiveConfigProvider {
    final ListProperty<String> ignoredEntries;
    final ConfigurableFileCollection mainResources;
    final MapProperty<String, String> forcedProperties;
    final MapProperty<String, Object> projectProperties;
    final MapProperty<String, String> quarkusBuildProperties;
    final MapProperty<String, String> quarkusRelevantProjectProperties;
    final MapProperty<String, Object> manifestAttributes;
    final MapProperty<String, Attributes> manifestSections;
    final Property<Boolean> nativeBuild;
    final Property<String> quarkusProfileSystemVariable;
    final Property<String> quarkusProfileEnvVariable;

    public EffectiveConfigProvider(ListProperty<String> ignoredEntries,
            ConfigurableFileCollection mainResources,
            MapProperty<String, String> forcedProperties,
            MapProperty<String, Object> projectProperties,
            MapProperty<String, String> quarkusBuildProperties,
            MapProperty<String, String> quarkusRelevantProjectProperties,
            MapProperty<String, Object> manifestAttributes,
            MapProperty<String, Attributes> manifestSections,
            Property<Boolean> nativeBuild,
            Property<String> quarkusProfileSystemVariable,
            Property<String> quarkusProfileEnvVariable) {
        this.ignoredEntries = ignoredEntries;
        this.mainResources = mainResources;
        this.forcedProperties = forcedProperties;
        this.projectProperties = projectProperties;
        this.quarkusBuildProperties = quarkusBuildProperties;
        this.quarkusRelevantProjectProperties = quarkusRelevantProjectProperties;
        this.manifestAttributes = manifestAttributes;
        this.manifestSections = manifestSections;
        this.nativeBuild = nativeBuild;
        this.quarkusProfileSystemVariable = quarkusProfileSystemVariable;
        this.quarkusProfileEnvVariable = quarkusProfileEnvVariable;
    }

    public EffectiveConfig buildEffectiveConfiguration(ApplicationModel appModel,
            Map<String, ?> additionalForcedProperties) {
        ResolvedDependency appArtifact = appModel.getAppArtifact();

        Map<String, Object> properties = new HashMap<>();
        exportCustomManifestProperties(properties);

        Map<String, String> defaultProperties = new HashMap<>();
        String userIgnoredEntries = String.join(",", ignoredEntries.get());
        if (!userIgnoredEntries.isEmpty()) {
            defaultProperties.put("quarkus.package.jar.user-configured-ignored-entries", userIgnoredEntries);
        }
        Set<File> resourcesDirs = mainResources.getFiles();
        defaultProperties.putIfAbsent("quarkus.application.name", appArtifact.getArtifactId());
        defaultProperties.putIfAbsent("quarkus.application.version", appArtifact.getVersion());

        Map<String, String> forced = new HashMap<>(forcedProperties.get());
        projectProperties.get().forEach((k, v) -> {
            forced.put(k, v.toString());

        });
        additionalForcedProperties.forEach((k, v) -> {
            forced.put(k, v.toString());
        });
        if (nativeBuild.get()) {
            forced.put("quarkus.native.enabled", "true");
        }
        return EffectiveConfig.builder()
                .withPlatformProperties(appModel.getPlatformProperties())
                .withForcedProperties(forced)
                .withTaskProperties(properties)
                .withBuildProperties(quarkusBuildProperties.get())
                .withProjectProperties(quarkusRelevantProjectProperties.get())
                .withDefaultProperties(defaultProperties)
                .withSourceDirectories(resourcesDirs)
                .withProfile(getQuarkusProfile())
                .build();
    }

    private void exportCustomManifestProperties(Map<String, Object> properties) {
        for (Map.Entry<String, Object> attribute : manifestAttributes.get().entrySet()) {
            properties.put(toManifestAttributeKey(attribute.getKey()),
                    attribute.getValue());
        }

        for (Map.Entry<String, Attributes> section : manifestSections.get().entrySet()) {
            for (Map.Entry<String, Object> attribute : section.getValue().entrySet()) {
                properties
                        .put(toManifestSectionAttributeKey(section.getKey(), attribute.getKey()), attribute.getValue());
            }
        }
    }

    private String getQuarkusProfile() {
        String profile = quarkusProfileSystemVariable.getOrNull();
        if (profile == null) {
            profile = quarkusProfileEnvVariable.getOrNull();
        }
        if (profile == null) {
            profile = quarkusBuildProperties.get().get(QUARKUS_PROFILE);
        }
        if (profile == null) {
            Object p = quarkusRelevantProjectProperties.get().get(QUARKUS_PROFILE);
            if (p != null) {
                profile = p.toString();
            }
        }
        if (profile == null) {
            profile = "prod";
        }
        return profile;
    }

}
