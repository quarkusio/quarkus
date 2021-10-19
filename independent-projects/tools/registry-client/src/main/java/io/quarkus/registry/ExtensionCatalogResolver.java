package io.quarkus.registry;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.ResolverState.Lazy;
import io.quarkus.registry.ResolverState.LazyResolver;
import io.quarkus.registry.ResolverState.ResolvingSupplier;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public abstract class ExtensionCatalogResolver {

    public static ArtifactCoords toPlatformBom(String bomVersion) {
        return toPlatformBom(null, null, bomVersion);
    }

    public static ArtifactCoords toPlatformBom(String bomGroupId, String bomArtifactId, String bomVersion) {
        if (bomVersion == null) {
            throw new IllegalArgumentException("BOM version was not provided");
        }

        return new ArtifactCoords(
                bomGroupId == null ? Constants.DEFAULT_PLATFORM_BOM_GROUP_ID : bomGroupId,
                bomArtifactId == null ? Constants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID : bomArtifactId,
                bomVersion);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        MessageWriter log;
        boolean enableDebug = false;

        Boolean useRegistryClient = null;
        boolean refreshCache;

        Lazy<RegistriesConfig> lazyConfig = ResolverState.lazy(
                () -> RegistriesConfigLocator.resolveConfig());

        LazyResolver<MavenArtifactResolver> lazyMvnResolver = ResolverState.lazyResolver(() -> {
            try {
                return MavenArtifactResolver.builder()
                        .setWorkspaceDiscovery(false)
                        .setArtifactTransferLogging(enableDebug)
                        .build();
            } catch (BootstrapMavenException e) {
                throw new IllegalStateException("Failed to initialize the default Maven artifact resolver", e);
            }
        });

        ArtifactCoords fallbackBom = null;

        /**
         * @param enableDebug If true, enable debug messages. Note this setting
         *        is ignored if a MessageWriter is set.
         * @see #withLog(MessageWriter)
         */
        public Builder withDebug(boolean enableDebug) {
            this.enableDebug = enableDebug;
            return this;
        }

        /**
         * @param log MessageWriter to use for logging. If this is set, this will be
         *        used to determine whether or not debug is enabled.
         * @see MessageWriter#isDebugEnabled()
         */
        public Builder withLog(MessageWriter log) {
            this.log = log;
            return this;
        }

        public Builder withRegistryClient(boolean useRegistryClient) {
            this.useRegistryClient = useRegistryClient;
            return this;
        }

        public Builder withConfigFile(Path configFilePath) {
            this.lazyConfig = ResolverState.lazy(() -> configFilePath == null
                    ? RegistriesConfigLocator.resolveConfig()
                    : RegistriesConfigLocator.load(configFilePath));
            return this;
        }

        public Builder withConfig(RegistriesConfig config) {
            this.lazyConfig = ResolverState.lazy(() -> config);
            return this;
        }

        public Builder withRefresh(boolean refreshCache) {
            this.refreshCache = refreshCache;
            return this;
        }

        public Builder withResolver(MavenArtifactResolver artifactResolver) {
            this.lazyMvnResolver = ResolverState.lazyResolver(() -> artifactResolver);
            return this;
        }

        public Builder withResolver(ResolvingSupplier<MavenArtifactResolver> artifactResolverSupplier) {
            this.lazyMvnResolver = ResolverState.lazyResolver(artifactResolverSupplier);
            return this;
        }

        /**
         * @param platform Quarkus platform version that should be used if
         *        the registry client is disabled or fails to resolve.
         */
        public Builder withFallbackVersion(ArtifactCoords platform) {
            Objects.requireNonNull(platform.getGroupId(), "Platform BOM groupId was not provided");
            Objects.requireNonNull(platform.getArtifactId(), "Platform BOM artifactId was not provided");
            Objects.requireNonNull(platform.getVersion(), "Platform BOM version was not provided");
            this.fallbackBom = platform;
            return this;
        }

        /**
         * Builder method.
         *
         * @return a catalog resolver that uses the provided attributes to
         *         resolve resources. Will not return null, will not throw.
         */
        public ExtensionCatalogResolver build() {
            if (useRegistryClient == null) {
                String value = System.getProperty("quarkusRegistryClient");
                if (value == null) {
                    value = System.getenv("QUARKUS_REGISTRY_CLIENT");
                }
                useRegistryClient = value == null || value.isBlank() || Boolean.parseBoolean(value);
            }

            if (log == null) {
                log = enableDebug ? MessageWriter.debug() : MessageWriter.info();
            } else {
                enableDebug = log.isDebugEnabled();
            }

            ExtensionCatalogResolver resolver;
            try {
                resolver = Objects.requireNonNull(useRegistryClient)
                        ? new ExtensionCatalogRegistryResolver(this)
                        : new ExtensionCatalogFallbackResolver(this);
            } catch (RegistryResolutionException e) {
                Optional<RegistriesConfig> value = lazyConfig.test();
                log.warn("Unable to resolve registries using the current client configuration: %s",
                        value.isPresent()
                                ? value.get().getSource().describe()
                                : String.format("Unknown. Cause: ", e.getMessage()));
                resolver = new ExtensionCatalogFallbackResolver(this);
            }

            if (refreshCache) {
                resolver.clearCache();
            }
            return resolver;
        }
    }

    protected final MessageWriter log;
    private final Lazy<RegistriesConfig> lazyConfig;
    private final LazyResolver<MavenArtifactResolver> lazyMvnResolver;

    protected ExtensionCatalogResolver(Builder builder) {
        this.log = builder.log;
        this.lazyConfig = builder.lazyConfig;
        this.lazyMvnResolver = builder.lazyMvnResolver;
    }

    /**
     * @return read-only Registry configuration
     */
    public RegistriesConfig getConfig() {
        return lazyConfig.get();
    }

    /**
     * @return MessageWriter for log messages
     * @see MessageWriter#isDebugEnabled()
     */
    public MessageWriter getMessageWriter() {
        return log;
    }

    protected abstract void clearCache();

    protected MavenArtifactResolver resolver() throws RegistryResolutionException {
        return lazyMvnResolver.get();
    }

    public abstract ExtensionCatalog resolveExtensionCatalog() throws RegistryResolutionException;

    public abstract ExtensionCatalog resolveExtensionCatalog(PlatformStreamCoords stream) throws RegistryResolutionException;

    public abstract ExtensionCatalog resolveExtensionCatalog(String quarkusCoreVersion) throws RegistryResolutionException;

    public abstract ExtensionCatalog resolveExtensionCatalog(Collection<ArtifactCoords> preferredPlatforms)
            throws RegistryResolutionException;

    public abstract PlatformCatalog resolvePlatformCatalog() throws RegistryResolutionException;

    public abstract PlatformCatalog resolvePlatformCatalog(String quarkusVersion) throws RegistryResolutionException;

    public abstract PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId) throws RegistryResolutionException;

    public abstract PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId, String quarkusVersion)
            throws RegistryResolutionException;
}
