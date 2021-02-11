package io.quarkus.registry;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.dependencies.ExtensionPredicate;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.registry.builder.RegistryBuilder;
import io.quarkus.registry.builder.URLRegistryBuilder;
import io.quarkus.registry.model.ArtifactKey;
import io.quarkus.registry.model.Extension.ExtensionRelease;
import io.quarkus.registry.model.Registry;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.immutables.value.Value;

/**
 * This {@link ExtensionRegistry} implementation uses in-memory {@link Registry}
 * objects to query the data.
 */
public class DefaultExtensionRegistry implements ExtensionRegistry {

    private final Registry registry;

    /**
     * Create a {@link DefaultExtensionRegistry} out of a {@link Collection} of {@link URL}s
     *
     * @param urls urls used to lookup this registry
     * @return a {@link DefaultExtensionRegistry} instance
     * @throws IOException if any IO error occurs while reading each URL contents
     */
    public static DefaultExtensionRegistry fromURLs(Collection<URL> urls) throws IOException {
        Registry registry = new URLRegistryBuilder()
                .addURLs(urls)
                .build();
        return new DefaultExtensionRegistry(registry);
    }

    /**
     * Create a {@link DefaultExtensionRegistry} from a single {@link QuarkusPlatformDescriptor}
     *
     * @param platform the single platform
     * @return a {@link DefaultExtensionRegistry} instance
     */
    public static DefaultExtensionRegistry fromPlatform(QuarkusPlatformDescriptor platform) {
        RegistryBuilder builder = new RegistryBuilder();
        builder.visitPlatform(platform);
        return new DefaultExtensionRegistry(builder.build());
    }

    public DefaultExtensionRegistry(Registry registry) {
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
    }

    @Override
    public Set<String> getQuarkusCoreVersions() {
        return registry.getCoreVersions().keySet().stream().map(ComparableVersion::toString).collect(
                Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<Extension> getExtensionsByCoreVersion(String version) {
        return list(version, "");
    }

    @Override
    public Set<Extension> list(String quarkusCore, String keyword) {
        return listInternalExtensions(quarkusCore, keyword)
                .stream()
                .map(this::toQuarkusExtension)
                .sorted(Comparator.comparing(Extension::getArtifactId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public ExtensionInstallPlan planInstallation(String quarkusCore, Collection<String> keywords) {
        ExtensionInstallPlan.Builder builder = ExtensionInstallPlan.builder();
        boolean multipleKeywords = keywords.size() > 1;
        for (String keyword : keywords) {
            int countColons = StringUtils.countMatches(keyword, ":");
            // Check if it's just groupId:artifactId
            if (countColons == 1) {
                AppArtifactKey artifactKey = AppArtifactKey.fromString(keyword);
                builder.addManagedExtension(new AppArtifactCoords(artifactKey, null));
                continue;
            } else if (countColons > 1) {
                // it's a gav
                builder.addIndependentExtension(AppArtifactCoords.fromString(keyword));
                continue;
            }
            List<ExtensionReleaseTuple> tuples = listInternalExtensions(quarkusCore, keyword);
            if (tuples.size() != 1 && multipleKeywords) {
                // No extension found for this keyword. Return empty immediately
                return ExtensionInstallPlan.EMPTY;
            }
            // If it's a pattern allow multiple results
            // See https://github.com/quarkusio/quarkus/issues/11086#issuecomment-666360783
            else if (tuples.size() > 1 && !ExtensionPredicate.isPattern(keyword)) {
                throw new MultipleExtensionsFoundException(keyword,
                        tuples.stream().map(this::toQuarkusExtension).collect(Collectors.toList()));
            }
            for (ExtensionReleaseTuple tuple : tuples) {
                ArtifactKey id = tuple.getExtension().getId();
                String groupId = id.getGroupId();
                String artifactId = id.getArtifactId();
                String version = tuple.getRelease().getRelease().getVersion();
                AppArtifactCoords extensionCoords = new AppArtifactCoords(groupId, artifactId, version);
                List<AppArtifactCoords> platformCoords = tuple.getRelease().getPlatforms()
                        .stream()
                        .map(c -> new AppArtifactCoords(
                                c.getCoords().getId().getGroupId(),
                                c.getCoords().getId().getArtifactId(),
                                "pom",
                                c.getCoords().getVersion()))
                        .collect(Collectors.toList());
                if (platformCoords.isEmpty()) {
                    builder.addIndependentExtension(extensionCoords);
                } else {
                    builder.addManagedExtension(extensionCoords);
                    for (AppArtifactCoords platformCoord : platformCoords) {
                        builder.addPlatform(platformCoord);
                    }
                }
            }
        }
        return builder.build();
    }

    private List<ExtensionReleaseTuple> listInternalExtensions(String quarkusCore, String keyword) {
        List<ExtensionReleaseTuple> result = new ArrayList<>();
        ExtensionPredicate predicate = null;
        if (keyword != null && !keyword.isEmpty()) {
            predicate = new ExtensionPredicate(keyword);
        }
        for (io.quarkus.registry.model.Extension extension : registry.getExtensions()) {
            for (ExtensionRelease extensionRelease : extension.getReleases()) {
                if (quarkusCore.equals(extensionRelease.getRelease().getQuarkusCore())) {
                    ExtensionReleaseTuple tuple = ExtensionReleaseTuple.builder().extension(extension)
                            .release(extensionRelease).build();
                    // If no filter is defined, just return the tuple
                    if (predicate == null) {
                        result.add(tuple);
                    } else {
                        Extension quarkusExtension = toQuarkusExtension(tuple);
                        // If there is an exact match, return only this
                        if (predicate.isExactMatch(quarkusExtension)) {
                            return Collections.singletonList(tuple);
                        } else if (predicate.test(quarkusExtension)) {
                            result.add(tuple);
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

    private Extension toQuarkusExtension(ExtensionReleaseTuple tuple) {
        io.quarkus.registry.model.Extension extension = tuple.getExtension();
        ExtensionRelease tupleRelease = tuple.getRelease();
        // Platforms may have metadata overriding the extension metadata
        Map<String, Object> metadata = new HashMap<>(extension.getMetadata());
        tupleRelease.getPlatforms().stream()
                .map(io.quarkus.registry.model.Extension.ExtensionPlatformRelease::getMetadata)
                .findFirst()
                .ifPresent(metadata::putAll);
        ArtifactKey id = extension.getId();
        return new Extension()
                .setGroupId(id.getGroupId())
                .setArtifactId(id.getArtifactId())
                .setVersion(tupleRelease.getRelease().getVersion())
                .setName(extension.getName())
                .setDescription(extension.getDescription())
                .setMetadata(metadata);
    }

    /**
     * Used in tests
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * This exists only because ExtensionRelease does not accept a back reference to Extension
     */
    @Value.Immutable
    interface ExtensionReleaseTuple {
        io.quarkus.registry.model.Extension getExtension();

        ExtensionRelease getRelease();

        static ImmutableExtensionReleaseTuple.Builder builder() {
            return ImmutableExtensionReleaseTuple.builder();
        }
    }
}
