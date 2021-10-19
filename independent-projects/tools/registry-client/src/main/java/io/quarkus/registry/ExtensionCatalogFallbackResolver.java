package io.quarkus.registry;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonCatalogMerger;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.selection.OriginPreference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * This resolver does not use the registry client for resolving artifacts.
 * It is used when using the registry client has been disabled, or when there
 * was a problem resolving the registry configuration.
 */
public class ExtensionCatalogFallbackResolver extends ExtensionCatalogResolver {

    ArtifactCoords fallbackBom;
    ExtensionCatalog platformCatalog;

    ExtensionCatalogFallbackResolver(Builder builder) {
        super(builder);
        Objects.requireNonNull(getConfig());
        this.fallbackBom = builder.fallbackBom;
    }

    @Override
    protected void clearCache() {
        log.warn("Skipping refresh. No registries are configured.");
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog() throws RegistryResolutionException {
        Objects.requireNonNull(fallbackBom,
                "The Quarkus platform groupId, artifactId, and version must be specified to resolve resources without the registry client.");

        if (platformCatalog == null) {
            platformCatalog = findPlatformJson(fallbackBom);
        }
        return platformCatalog;
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog(PlatformStreamCoords stream) throws RegistryResolutionException {
        throw new RegistryResolutionException("Unable to resolve the Platform stream. No registries are configured.");
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog(String quarkusVersion) throws RegistryResolutionException {
        ArtifactCoords coords = toPlatformBom(quarkusVersion);
        if (coords.equals(fallbackBom)) {
            return resolveExtensionCatalog();
        }
        return findPlatformJson(coords);
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog(Collection<ArtifactCoords> preferredPlatforms)
            throws RegistryResolutionException {
        // ToolsUtils.mergePlatforms(collectImportedPlatforms, mvnResolver);
        // ToolsUtils.mergePlatforms(getImportedPlatforms, mvnResolver);

        //        if (!catalogResolver.hasRegistries()) {
        //            groupId = getPlatformGroupId(mojo, groupId);
        //            artifactId = getPlatformArtifactId(artifactId);
        //            version = getPlatformVersion(mojo, version);
        //            final StringBuilder buf = new StringBuilder();
        //            buf.append("The extension catalog will be narrowed to the ").append(groupId).append(":").append(artifactId)
        //                    .append(":").append(version).append(" platform release.");
        //            buf.append(
        //                    " To enable the complete Quarkiverse extension catalog along with the latest recommended platform releases, please, make sure ");
        //            if (QuarkusProjectHelper.isRegistryClientEnabled()) {
        //                buf.append("the following registries are accessible from your environment: ");
        //                final Iterator<RegistryConfig> iterator = RegistriesConfigLocator.resolveConfig().getRegistries().iterator();
        //                int i = 0;
        //                while (iterator.hasNext()) {
        //                    final RegistryConfig r = iterator.next();
        //                    if (r.isEnabled()) {
        //                        if (i++ > 0) {
        //                            buf.append(", ");
        //                        }
        //                        buf.append(r.getId());
        //                    }
        //                }
        //
        //            } else {
        //                buf.append("the extension registry client is enabled.");
        //            }
        //            log.warn(buf.toString());
        //            return ToolsUtils.resolvePlatformDescriptorDirectly(groupId, artifactId, version, artifactResolver, log);
        //        }

        return null;
    }

    @Override
    public PlatformCatalog resolvePlatformCatalog() throws RegistryResolutionException {
        return null;
    }

    @Override
    public PlatformCatalog resolvePlatformCatalog(String quarkusVersion) throws RegistryResolutionException {
        ArtifactCoords coords = toPlatformBom(quarkusVersion);
        return null;
    }

    @Override
    public PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId) throws RegistryResolutionException {
        return null;
    }

    @Override
    public PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId, String quarkusVersion)
            throws RegistryResolutionException {
        return null;
    }

    protected ExtensionCatalog findPlatformJson(ArtifactCoords platformCoords) throws RegistryResolutionException {
        MavenArtifactResolver artifactResolver = resolver();
        Artifact catalogCoords = new DefaultArtifact(
                platformCoords.getGroupId(),
                platformCoords.getArtifactId() + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX,
                platformCoords.getVersion(), "json", platformCoords.getVersion());

        Path platformJson = null;
        ExtensionCatalog catalog = null;

        try {
            log.debug("Resolving platform descriptor %s", catalogCoords);
            platformJson = artifactResolver.resolve(catalogCoords).getArtifact().getFile().toPath();
        } catch (Exception e) {
            if (Constants.DEFAULT_PLATFORM_BOM_GROUP_ID.equals(platformCoords.getGroupId())
                    && catalogCoords.getArtifactId().startsWith("quarkus-bom")) {
                // If we had io.quarkus.platform:quarkus-bom*, let's try again with io.quarkus:quarkus-platform*
                catalogCoords = new DefaultArtifact(
                        Constants.IO_QUARKUS,
                        "quarkus-bom" + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX,
                        catalogCoords.getClassifier(), catalogCoords.getExtension(), catalogCoords.getVersion());
                try {
                    log.debug("Resolving platform descriptor %s", catalogCoords);
                    platformJson = artifactResolver.resolve(catalogCoords).getArtifact().getFile().toPath();
                } catch (Exception e2) {
                }
            }
            if (platformJson == null) {
                throw new RegistryResolutionException("Failed to resolve the default platform JSON descriptor", e);
            }
        }

        try {
            catalog = JsonCatalogMapperHelper.deserialize(platformJson, JsonExtensionCatalog.class);
        } catch (IOException e) {
            throw new RegistryResolutionException("Failed to deserialize extension catalog " + platformJson, e);
        }

        Map<String, Object> md = catalog.getMetadata();
        if (md != null) {
            Object o = md.get("platform-release");
            if (o instanceof Map) {
                Object members = ((Map<?, ?>) o).get("members");
                if (members instanceof Collection) {
                    final Collection<?> memberList = (Collection<?>) members;
                    final List<ExtensionCatalog> catalogs = new ArrayList<>(memberList.size());

                    int memberIndex = 0;
                    for (Object m : memberList) {
                        if (!(m instanceof String)) {
                            continue;
                        }
                        final ExtensionCatalog memberCatalog;
                        if (catalog.getId().equals(m)) {
                            memberCatalog = catalog;
                        } else {
                            try {
                                final ArtifactCoords coords = ArtifactCoords.fromString((String) m);
                                catalogCoords = new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                                        coords.getClassifier(), coords.getType(), coords.getVersion());
                                log.debug("Resolving platform descriptor %s", catalogCoords);
                                final Path jsonPath = artifactResolver.resolve(catalogCoords).getArtifact().getFile().toPath();
                                memberCatalog = JsonCatalogMapperHelper.deserialize(jsonPath,
                                        JsonExtensionCatalog.class);
                            } catch (Exception e) {
                                log.warn("Failed to resolve member catalog " + m, e);
                                continue;
                            }
                        }

                        final OriginPreference originPreference = new OriginPreference(1, 1, 1, ++memberIndex, 1);
                        Map<String, Object> metadata = new HashMap<>(memberCatalog.getMetadata());
                        metadata.put("origin-preference", originPreference);
                        ((JsonExtensionCatalog) memberCatalog).setMetadata(metadata);
                        catalogs.add(memberCatalog);
                    }
                    catalog = JsonCatalogMerger.merge(catalogs);
                }
            }
        }
        return catalog;
    }

    //    public static ExtensionCatalog mergePlatforms(List<ArtifactCoords> platforms, MavenArtifactResolver artifactResolver) {
    //        // TODO remove this method once we have the registry service available
    //        return mergePlatforms(platforms, new BootstrapAppModelResolver(artifactResolver));
    //    }
    //
    //    public static ExtensionCatalog mergePlatforms(List<ArtifactCoords> platforms, AppModelResolver artifactResolver) {
    //        // TODO remove this method once we have the registry service available
    //        List<ExtensionCatalog> catalogs = new ArrayList<>(platforms.size());
    //        for (ArtifactCoords platform : platforms) {
    //            final Path json;
    //            try {
    //                json = artifactResolver.resolve(new GACTV(platform.getGroupId(), platform.getArtifactId(),
    //                        platform.getClassifier(), platform.getType(), platform.getVersion())).getResolvedPaths()
    //                        .getSinglePath();
    //            } catch (Exception e) {
    //                throw new RuntimeException("Failed to resolve platform descriptor " + platform, e);
    //            }
    //            try {
    //                catalogs.add(JsonCatalogMapperHelper.deserialize(json, JsonExtensionCatalog.class));
    //            } catch (IOException e) {
    //                throw new RuntimeException("Failed to deserialize platform descriptor " + json, e);
    //            }
    //        }
    //        return JsonCatalogMerger.merge(catalogs);
    //    }

    //    groupId = getPlatformGroupId(mojo, groupId);
    //    artifactId = getPlatformArtifactId(artifactId);
    //    version = getPlatformVersion(mojo, version);
    //    final StringBuilder buf = new StringBuilder();
    //            buf.append("The extension catalog will be narrowed to the ").append(groupId).append(":").append(artifactId)
    //                    .append(":").append(version).append(" platform release.");
    //            buf.append(
    //                    " To enable the complete Quarkiverse extension catalog along with the latest recommended platform releases, please, make sure ");
    //            if (QuarkusProjectHelper.isRegistryClientEnabled()) {
    //        buf.append("the following registries are accessible from your environment: ");
    //        final Iterator<RegistryConfig> iterator = RegistriesConfigLocator.resolveConfig().getRegistries().iterator();
    //        int i = 0;
    //        while (iterator.hasNext()) {
    //            final RegistryConfig r = iterator.next();
    //            if (r.isEnabled()) {
    //                if (i++ > 0) {
    //                    buf.append(", ");
    //                }
    //                buf.append(r.getId());
    //            }
    //        }
    //
    //    } else {
    //        buf.append("the extension registry client is enabled.");
    //    }
    //            log.warn(buf.toString());
    //            return ToolsUtils.resolvePlatformDescriptorDirectly(groupId, artifactId, version, artifactResolver, log);

}
