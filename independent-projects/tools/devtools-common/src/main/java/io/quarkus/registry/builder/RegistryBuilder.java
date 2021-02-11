package io.quarkus.registry.builder;

import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.catalog.spi.IndexVisitor;
import io.quarkus.registry.model.ArtifactCoords;
import io.quarkus.registry.model.ArtifactKey;
import io.quarkus.registry.model.Extension.ExtensionPlatformRelease;
import io.quarkus.registry.model.ImmutableArtifactCoords;
import io.quarkus.registry.model.ImmutableArtifactKey;
import io.quarkus.registry.model.ImmutableExtensionPlatformRelease;
import io.quarkus.registry.model.ImmutableRegistry;
import io.quarkus.registry.model.ModifiableExtension;
import io.quarkus.registry.model.ModifiableExtensionRelease;
import io.quarkus.registry.model.ModifiablePlatform;
import io.quarkus.registry.model.Registry;
import io.quarkus.registry.model.Release;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.immutables.value.Value;

public class RegistryBuilder implements IndexVisitor {

    private final Map<ArtifactKey, ModifiablePlatform> platforms = new LinkedHashMap<>();

    private final Map<ArtifactKey, ModifiableExtension> extensions = new LinkedHashMap<>();

    private final Map<ArtifactCoordsTuple, ModifiableExtensionRelease> releases = new LinkedHashMap<>();

    private final ImmutableRegistry.Builder registryBuilder = Registry.builder();

    @Override
    public void visitPlatform(QuarkusPlatformDescriptor platform) {
        registryBuilder.putCoreVersions(new ComparableVersion(platform.getQuarkusVersion()), new HashMap<>());
        registryBuilder.addAllCategories(platform.getCategories());

        ArtifactKey platformKey = ImmutableArtifactKey.of(platform.getBomGroupId(), platform.getBomArtifactId());
        ModifiablePlatform platformBuilder = platforms.computeIfAbsent(platformKey,
                key -> ModifiablePlatform.create().setId(key));

        platformBuilder.addReleases(Release.builder().version(platform.getBomVersion())
                .quarkusCore(platform.getQuarkusVersion())
                .build());

        ArtifactCoords platformCoords = ImmutableArtifactCoords.of(platformKey, platform.getBomVersion());
        for (Extension extension : platform.getExtensions()) {
            visitExtension(extension, platform.getQuarkusVersion(), platformCoords);
        }
    }

    @Override
    public void visitExtension(Extension extension, String quarkusCore) {
        visitExtension(extension, quarkusCore, null);
    }

    private void visitExtension(Extension extension, String quarkusCore, ArtifactCoords platform) {
        // Ignore unlisted extensions
        if (extension.isUnlisted()) {
            return;
        }
        ArtifactKey extensionKey = ImmutableArtifactKey.of(extension.getGroupId(), extension.getArtifactId());
        ModifiableExtension extensionBuilder = extensions
                .computeIfAbsent(extensionKey, key -> ModifiableExtension.create()
                        .setId(extensionKey)
                        .setName(Objects.toString(extension.getName(), extension.getArtifactId()))
                        .setDescription(extension.getDescription())
                        .setMetadata(extension.getMetadata()));
        ArtifactCoords coords = ImmutableArtifactCoords.of(extensionKey, extension.getVersion());
        ArtifactCoordsTuple key = ImmutableArtifactCoordsTuple.builder().coords(coords)
                .quarkusVersion(quarkusCore)
                .build();
        ModifiableExtensionRelease releaseBuilder = releases.computeIfAbsent(key,
                appArtifactCoords -> ModifiableExtensionRelease.create()
                        .setRelease(Release.builder()
                                .version(appArtifactCoords.getCoords().getVersion())
                                .quarkusCore(appArtifactCoords.getQuarkusVersion())
                                .build()));
        if (platform != null) {
            Map<String, Object> metadata = diff(extensionBuilder.getMetadata(), extension.getMetadata());
            ExtensionPlatformRelease platformRelease = ImmutableExtensionPlatformRelease
                    .builder().coords(platform).metadata(metadata).build();
            releaseBuilder.addPlatforms(platformRelease);
        }
    }

    public Registry build() {
        for (Map.Entry<ArtifactCoordsTuple, ModifiableExtensionRelease> entry : releases.entrySet()) {
            ArtifactCoordsTuple tuple = entry.getKey();
            ModifiableExtensionRelease extensionReleaseBuilder = entry.getValue();
            ArtifactKey key = tuple.getCoords().getId();
            ModifiableExtension extensionBuilder = extensions.get(key);
            extensionBuilder.addReleases(extensionReleaseBuilder.toImmutable());
        }
        extensions.values().stream().map(ModifiableExtension::toImmutable).forEach(registryBuilder::addExtensions);
        platforms.values().stream().map(ModifiablePlatform::toImmutable).forEach(registryBuilder::addPlatforms);
        return registryBuilder.build();
    }

    static Map<String, Object> diff(Map<String, Object> left, Map<String, Object> right) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : right.entrySet()) {
            Object value = left.get(entry.getKey());
            if (!entry.getValue().equals(value)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    @Value.Immutable
    public interface ArtifactCoordsTuple {
        @Value.Parameter
        ArtifactCoords getCoords();

        @Value.Parameter
        String getQuarkusVersion();
    }

}
