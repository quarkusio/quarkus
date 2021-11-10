package io.quarkus.registry;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ExtensionCatalogRegistryResolver extends ExtensionCatalogResolver {

    private final List<RegistryExtensionResolver> registries;
    private final RegistriesConfig config;
    private final MavenArtifactResolver resolver;

    ExtensionCatalogRegistryResolver(Builder builder) throws RegistryResolutionException {
        super(builder);

        // Force resolution and generate the build clients
        this.config = Objects.requireNonNull(getConfig());
        this.resolver = resolver();

        // Generate registry clients
        this.registries = resolveRegistryClients(config);
    }

    protected List<RegistryExtensionResolver> resolveRegistryClients(RegistriesConfig config)
            throws RegistryResolutionException {
        List<RegistryExtensionResolver> registries = new ArrayList<>(config.getRegistries().size());
        RegistryClientFactoryResolver clientFactoryResolver = new RegistryClientFactoryResolver(this.resolver,
                this.log);
        for (RegistryConfig registry : config.getRegistries()) {
            if (!registry.isEnabled()) {
                continue;
            }
            RegistryClientFactory clientFactory = clientFactoryResolver.getClientFactory(registry);

            registries.add(new RegistryExtensionResolver(
                    clientFactory.buildRegistryClient(registry), log, registries.size()));
        }
        return registries;
    }

    @Override
    protected void clearCache() {
        log.debug("Refreshing registry cache");
        try {
            for (RegistryExtensionResolver registry : registries) {
                registry.clearCache();
            }
        } catch (Exception e) {
            log.warn("Unable to refresh the registry cache: %s", e.getMessage());
        }
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog() {
        return null;
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog(PlatformStreamCoords stream) throws RegistryResolutionException {
        return null;
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog(String quarkusCoreVersion) throws RegistryResolutionException {
        return null;
    }

    @Override
    public ExtensionCatalog resolveExtensionCatalog(Collection<ArtifactCoords> preferredPlatforms)
            throws RegistryResolutionException {
        return null;
    }

    @Override
    public PlatformCatalog resolvePlatformCatalog() {
        return null;
    }

    @Override
    public PlatformCatalog resolvePlatformCatalog(String quarkusVersion) throws RegistryResolutionException {
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

}
