package io.quarkus.registry;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.aether.artifact.DefaultArtifact;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.maven.MavenRegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.client.spi.RegistryClientFactoryProvider;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.util.PlatformArtifacts;

public class ExtensionCatalogResolver {

    public static ExtensionCatalogResolver empty() {
        final ExtensionCatalogResolver resolver = new ExtensionCatalogResolver();
        resolver.registries = Collections.emptyList();
        resolver.log = MessageWriter.info();
        return resolver;
    }

    public static Builder builder() {
        return new ExtensionCatalogResolver().new Builder();
    }

    public class Builder {

        private MavenArtifactResolver artifactResolver;
        private boolean built;

        private RegistryClientFactory defaultClientFactory;
        private RegistryClientEnvironment clientEnv;

        private Builder() {
        }

        public Builder artifactResolver(MavenArtifactResolver resolver) {
            assertNotBuilt();
            artifactResolver = resolver;
            return this;
        }

        public Builder messageWriter(MessageWriter messageWriter) {
            assertNotBuilt();
            log = messageWriter;
            return this;
        }

        public Builder config(RegistriesConfig registriesConfig) {
            assertNotBuilt();
            config = registriesConfig;
            return this;
        }

        public ExtensionCatalogResolver build() throws RegistryResolutionException {
            assertNotBuilt();
            built = true;
            completeConfig();
            buildRegistryClients();
            return ExtensionCatalogResolver.this;
        }

        private void completeConfig() {
            if (config == null) {
                config = RegistriesConfigLocator.resolveConfig();
            }
            if (log == null) {
                log = config.isDebug() ? MessageWriter.debug() : MessageWriter.info();
            }
            if (artifactResolver == null) {
                try {
                    artifactResolver = MavenArtifactResolver.builder()
                            .setWorkspaceDiscovery(false)
                            .setArtifactTransferLogging(config.isDebug())
                            .build();
                } catch (BootstrapMavenException e) {
                    throw new IllegalStateException("Failed to intialize the default Maven artifact resolver", e);
                }
            }
        }

        private void buildRegistryClients() throws RegistryResolutionException {
            registries = new ArrayList<>(config.getRegistries().size());
            for (RegistryConfig config : config.getRegistries()) {
                if (!config.isEnabled()) {
                    continue;
                }
                final RegistryClientFactory clientFactory = getClientFactory(config);
                registries
                        .add(new RegistryExtensionResolver(clientFactory.buildRegistryClient(config), log));
            }
        }

        private RegistryClientFactory getClientFactory(RegistryConfig config) {
            if (config.getExtra().isEmpty()) {
                return getDefaultClientFactory();
            }
            Object provider = config.getExtra().get("client-factory-artifact");
            if (provider != null) {
                return loadFromArtifact(config, provider);
            }
            provider = config.getExtra().get("client-factory-url");
            if (provider != null) {
                final URL url;
                try {
                    url = new URL((String) provider);
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("Failed to translate " + provider + " to URL", e);
                }
                return loadFromUrl(url);
            }
            return getDefaultClientFactory();
        }

        public RegistryClientFactory loadFromArtifact(RegistryConfig config, final Object providerValue) {
            ArtifactCoords providerArtifact;
            try {
                final String providerStr = (String) providerValue;
                providerArtifact = ArtifactCoords.fromString(providerStr);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to process configuration of " + config.getId()
                        + " registry: failed to cast " + providerValue + " to String", e);
            }
            final File providerJar;
            try {
                providerJar = artifactResolver.resolve(new DefaultArtifact(providerArtifact.getGroupId(),
                        providerArtifact.getArtifactId(), providerArtifact.getClassifier(),
                        providerArtifact.getType(), providerArtifact.getVersion())).getArtifact().getFile();
            } catch (BootstrapMavenException e) {
                throw new IllegalStateException(
                        "Failed to resolve the registry client factory provider artifact " + providerArtifact, e);
            }
            log.debug("Loading registry client factory for %s from %s", config.getId(), providerArtifact);
            final URL url;
            try {
                url = providerJar.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to translate " + providerJar + " to URL", e);
            }
            return loadFromUrl(url);
        }

        private RegistryClientFactory loadFromUrl(final URL url) {
            try {
                // here we use the classloader that loaded RegistryClientFactoryProvider instead of the TCCL
                // because, apparently, the TCCL doesn't work for this use-case from Maven plugins with extensions enabled
                final ClassLoader providerCl = new URLClassLoader(new URL[] { url },
                        RegistryClientFactoryProvider.class.getClassLoader());
                final Iterator<RegistryClientFactoryProvider> i = ServiceLoader
                        .load(RegistryClientFactoryProvider.class, providerCl).iterator();
                if (!i.hasNext()) {
                    throw new Exception("Failed to locate an implementation of " + RegistryClientFactoryProvider.class.getName()
                            + " service provider");
                }
                final RegistryClientFactoryProvider provider = i.next();
                if (i.hasNext()) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Found more than one registry client factory provider ")
                            .append(provider.getClass().getName());
                    while (i.hasNext()) {
                        buf.append(", ").append(i.next().getClass().getName());
                    }
                    throw new Exception(buf.toString());
                }
                return provider.newRegistryClientFactory(getClientEnv());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load registry client factory from " + url, e);
            }
        }

        private RegistryClientFactory getDefaultClientFactory() {
            return defaultClientFactory == null ? defaultClientFactory = new MavenRegistryClientFactory(artifactResolver, log)
                    : defaultClientFactory;
        }

        private RegistryClientEnvironment getClientEnv() {
            return clientEnv == null ? clientEnv = new RegistryClientEnvironment() {

                @Override
                public MessageWriter log() {
                    return log;
                }

                @Override
                public MavenArtifactResolver resolver() {
                    return artifactResolver;
                }
            } : clientEnv;
        }

        private void assertNotBuilt() {
            if (built) {
                throw new IllegalStateException("The builder has already built an instance");
            }
        }
    }

    private MessageWriter log;
    private RegistriesConfig config;
    private List<RegistryExtensionResolver> registries;

    public RegistriesConfig getConfig() {
        return config;
    }

    public boolean hasRegistries() {
        return !registries.isEmpty();
    }

    public PlatformCatalog resolvePlatformCatalog() throws RegistryResolutionException {
        return resolvePlatformCatalog(null);
    }

    public PlatformCatalog resolvePlatformCatalog(String quarkusVersion) throws RegistryResolutionException {

        List<PlatformCatalog> catalogs = new ArrayList<>(registries.size());
        for (RegistryExtensionResolver qer : registries) {
            final PlatformCatalog catalog = qer.resolvePlatformCatalog(quarkusVersion);
            if (catalog != null) {
                catalogs.add(catalog);
            }
        }

        if (catalogs.isEmpty()) {
            return null;
        }
        if (catalogs.size() == 1) {
            return catalogs.get(0);
        }

        final List<Platform> collectedPlatforms = new ArrayList<>();
        final Set<String> collectedPlatformKeys = new HashSet<>();

        String lastUpdated = null;
        boolean sawUnknownLastUpdate = false;

        for (PlatformCatalog c : catalogs) {
            collectPlatforms(c, collectedPlatforms, collectedPlatformKeys);

            if (!sawUnknownLastUpdate) {
                final Object catalogLastUpdated = c.getMetadata().get(Constants.LAST_UPDATED);
                if (catalogLastUpdated == null) {
                    // if for one of the catalogs it's unknown, it's going to be unknown for the merged catalog
                    lastUpdated = null;
                    sawUnknownLastUpdate = true;
                } else if (lastUpdated == null) {
                    lastUpdated = catalogLastUpdated.toString();
                } else if (lastUpdated.compareTo(catalogLastUpdated.toString()) < 0) {
                    lastUpdated = catalogLastUpdated.toString();
                }
            }
        }

        final PlatformCatalog.Mutable result = PlatformCatalog.builder();
        if (lastUpdated != null) {
            result.getMetadata().put(Constants.LAST_UPDATED, lastUpdated);
        }
        result.setPlatforms(collectedPlatforms);
        return result.build();
    }

    private void collectPlatforms(PlatformCatalog catalog, List<Platform> collectedPlatforms,
            Set<String> collectedPlatformKeys) {
        for (Platform p : catalog.getPlatforms()) {
            if (collectedPlatformKeys.add(p.getPlatformKey())) {
                collectedPlatforms.add(p);
            }
        }
    }

    public PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId) throws RegistryResolutionException {
        return getRegistryExtensionResolver(registryId).resolvePlatformCatalog();
    }

    public PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId, String quarkusVersion)
            throws RegistryResolutionException {
        return quarkusVersion == null ? resolvePlatformCatalogFromRegistry(registryId)
                : getRegistryExtensionResolver(registryId).resolvePlatformCatalog(quarkusVersion);
    }

    private RegistryExtensionResolver getRegistryExtensionResolver(String registryId) {
        for (var registryExtResolver : registries) {
            if (registryExtResolver.getId().equals(registryId)) {
                return registryExtResolver;
            }
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("Failed to locate ").append(registryId).append(" among the configured registries:");
        registries.forEach(r -> buf.append(" ").append(r.getId()));
        throw new IllegalStateException(buf.toString());
    }

    private class ExtensionCatalogBuilder {
        private final List<ExtensionCatalog.Mutable> catalogs = new ArrayList<>();
        final Map<String, List<RegistryExtensionResolver>> registriesByQuarkusCore = new HashMap<>();
        private final Map<String, Integer> compatibilityCodes = new LinkedHashMap<>();
        final List<String> upstreamQuarkusVersions = new ArrayList<>(1);
        final PlatformPreferenceIndex platformPreferenceIndex = new PlatformPreferenceIndex();
        final List<String> registryPreferenceIndex = new ArrayList<>(2);

        int getRegistryPreferenceIndex(String registryId) {
            for (int i = 0; i < registryPreferenceIndex.size(); ++i) {
                if (registryPreferenceIndex.get(i).equals(registryId)) {
                    return i;
                }
            }
            registryPreferenceIndex.add(registryId);
            return registryPreferenceIndex.size() - 1;
        }

        PlatformReleasePreferenceIndex getPlatformPreferenceIndex(int registryIndex, String platformKey) {
            return platformPreferenceIndex.getReleaseIndex(registryIndex, platformKey);
        }

        void addCatalog(ExtensionCatalog.Mutable c) {
            catalogs.add(c);
        }

        void addUpstreamQuarkusVersion(String quarkusVersion) {
            if (!upstreamQuarkusVersions.contains(quarkusVersion)) {
                upstreamQuarkusVersions.add(quarkusVersion);
            }
        }

        List<RegistryExtensionResolver> getRegistriesForQuarkusCore(String quarkusVersion) {
            return registriesByQuarkusCore.computeIfAbsent(quarkusVersion,
                    ExtensionCatalogResolver.this::getRegistriesForQuarkusVersion);
        }

        public int getCompatibilityCode(String quarkusVersion) {
            return getCompatibilityCode(quarkusVersion, null);
        }

        public int getCompatibilityCode(String quarkusVersion, String upstreamQuarkusVersion) {
            Integer i = compatibilityCodes.get(quarkusVersion);
            if (i == null) {
                if (upstreamQuarkusVersion != null) {
                    i = compatibilityCodes.computeIfAbsent(upstreamQuarkusVersion, cc -> compatibilityCodes.size());
                } else {
                    i = compatibilityCodes.size();
                }
                compatibilityCodes.put(quarkusVersion, i);
            }
            return i;
        }

        void appendAllNonPlatformExtensions() throws RegistryResolutionException {
            for (String quarkusVersion : compatibilityCodes.keySet()) {
                appendNonPlatformExtensions(this, quarkusVersion);
            }
        }

        void addUpstreamExtensionCatalogs(String quarkusCoreVersion, Set<String> processedPlatformKeys)
                throws RegistryResolutionException {
            collectPlatformExtensions(quarkusCoreVersion, this, processedPlatformKeys);
            int i = 0;
            while (i < upstreamQuarkusVersions.size()) {
                collectPlatformExtensions(upstreamQuarkusVersions.get(i++), this, processedPlatformKeys);
            }
            upstreamQuarkusVersions.clear();
        }

        ExtensionCatalog build() throws RegistryResolutionException {
            appendAllNonPlatformExtensions();
            if (catalogs.isEmpty()) {
                final List<RegistryExtensionResolver> registries = ExtensionCatalogResolver.this.registries;
                if (registries.isEmpty()) {
                    throw new RegistryResolutionException("Quarkus extension registry is not available");
                }
                if (registries.size() == 1) {
                    throw new RegistryResolutionException("Quarkus extension registry " + registries.get(0).getId()
                            + " did not provide any extension catalog");
                }
                final StringBuilder buf = new StringBuilder();
                buf.append("Quarkus extension registries ");
                buf.append(registries.get(0).getId());
                for (int i = 1; i < registries.size(); ++i) {
                    buf.append(", ").append(registries.get(i).getId());
                }
                buf.append(" did not provide any extension catalog");
                throw new RegistryResolutionException(buf.toString());
            }

            return CatalogMergeUtility.merge(catalogs);
        }
    }

    public ExtensionCatalog resolveExtensionCatalog() throws RegistryResolutionException {

        ensureRegistriesConfigured();

        final ExtensionCatalogBuilder catalogBuilder = new ExtensionCatalogBuilder();

        for (var registry : registries) {
            collectPlatformExtensions(catalogBuilder, registry);
        }

        return catalogBuilder.build();
    }

    private void collectPlatformExtensions(final ExtensionCatalogBuilder catalogBuilder, RegistryExtensionResolver registry)
            throws RegistryResolutionException {

        final PlatformCatalog pc = resolvePlatformCatalog(registry, catalogBuilder.upstreamQuarkusVersions);
        if (pc == null) {
            return;
        }

        final int registryPreferenceIndex = catalogBuilder.getRegistryPreferenceIndex(registry.getId());
        for (Platform platform : pc.getPlatforms()) {
            final PlatformReleasePreferenceIndex releasePreferenceIndex = catalogBuilder
                    .getPlatformPreferenceIndex(registryPreferenceIndex, platform.getPlatformKey());
            final int platformIndex = releasePreferenceIndex.getPlatformIndex();
            for (PlatformStream stream : platform.getStreams()) {
                for (PlatformRelease release : stream.getReleases()) {
                    final int releaseIndex = releasePreferenceIndex.getReleaseIndex(release.getVersion().toString());
                    final String quarkusVersion = release.getQuarkusCoreVersion();
                    final int compatiblityCode = catalogBuilder.getCompatibilityCode(quarkusVersion,
                            release.getUpstreamQuarkusCoreVersion());

                    if (!registry.isExclusiveProviderOf(quarkusVersion)) {
                        catalogBuilder.addUpstreamQuarkusVersion(quarkusVersion);
                    }
                    if (release.getUpstreamQuarkusCoreVersion() != null) {
                        catalogBuilder.addUpstreamQuarkusVersion(release.getUpstreamQuarkusCoreVersion());
                    }

                    int memberIndex = 0;
                    for (ArtifactCoords bom : release.getMemberBoms()) {
                        memberIndex++;
                        final ExtensionCatalog.Mutable ec = registry.resolvePlatformExtensions(bom);
                        if (ec != null) {
                            final OriginPreference originPreference = new OriginPreference(registryPreferenceIndex,
                                    platformIndex,
                                    releaseIndex, memberIndex, compatiblityCode);
                            addOriginPreference(ec, originPreference);
                            catalogBuilder.addCatalog(ec);
                        } else {
                            log.warn("Failed to resolve extension catalog for %s from registry %s", bom, registry.getId());
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves a platform catalog from a given registry. The method may return null in case the registry does not
     * provide platforms.
     *
     * <p>
     * It starts by resolving the default platform catalog. If a list of Quarkus core versions is provided,
     * it will also resolve platform catalogs for each Quarkus core version (if the registry recognizes
     * those Quarkus core versions), merge all the resolved platform catalogs into a single one and return the result.
     *
     * @param registry the registry to resolve the catalogs from
     * @param quarkusVersions optional extra Quarkus core versions
     * @return platform catalog or null if the registry does not provide any platforms
     * @throws RegistryResolutionException in case a registry querying failed or some other error happened
     */
    private static PlatformCatalog resolvePlatformCatalog(RegistryExtensionResolver registry, List<String> quarkusVersions)
            throws RegistryResolutionException {
        // default registry recommendations
        PlatformCatalog defaultCatalog = registry.resolvePlatformCatalog();
        if (quarkusVersions.isEmpty()) {
            return defaultCatalog;
        }

        List<PlatformCatalog> catalogsToMerge = List.of();
        for (int i = 0; i < quarkusVersions.size(); ++i) {
            var quarkusVersion = quarkusVersions.get(i);
            if (registry.isAcceptsQuarkusVersionQueries(quarkusVersion)) {
                final PlatformCatalog pcForQuarkusVersion = registry.resolvePlatformCatalog(quarkusVersion);
                if (pcForQuarkusVersion != null) {
                    if (catalogsToMerge.isEmpty()) {
                        catalogsToMerge = new ArrayList<>(quarkusVersions.size() - i + 1);
                        if (defaultCatalog != null) {
                            catalogsToMerge.add(defaultCatalog);
                        }
                    }
                    catalogsToMerge.add(pcForQuarkusVersion);
                }
            }
        }

        if (catalogsToMerge.isEmpty()) {
            return defaultCatalog;
        }
        return CatalogMergeUtility.mergePlatformCatalogs(catalogsToMerge);
    }

    private static void addOriginPreference(final ExtensionCatalog.Mutable ec, OriginPreference originPreference) {
        ec.getMetadata().put(Constants.REGISTRY_CLIENT_ORIGIN_PREFERENCE, originPreference);
    }

    public ExtensionCatalog resolveExtensionCatalog(String quarkusCoreVersion) throws RegistryResolutionException {
        if (quarkusCoreVersion == null) {
            return resolveExtensionCatalog();
        }

        final int registriesTotal = registries.size();
        if (registriesTotal == 0) {
            throw new RegistryResolutionException("No registries configured");
        }

        return resolveExtensionCatalog(quarkusCoreVersion, new ExtensionCatalogBuilder(), Collections.emptySet());
    }

    private ExtensionCatalog resolveExtensionCatalog(String quarkusCoreVersion,
            final ExtensionCatalogBuilder catalogBuilder, Set<String> preferredPlatformKeys)
            throws RegistryResolutionException {
        collectPlatformExtensions(quarkusCoreVersion, catalogBuilder, preferredPlatformKeys);
        int i = 0;
        while (i < catalogBuilder.upstreamQuarkusVersions.size()) {
            collectPlatformExtensions(catalogBuilder.upstreamQuarkusVersions.get(i++), catalogBuilder, preferredPlatformKeys);
        }
        return catalogBuilder.build();
    }

    public ExtensionCatalog resolveExtensionCatalog(PlatformStreamCoords streamCoords) throws RegistryResolutionException {

        ensureRegistriesConfigured();

        var catalog = resolveExtensionCatalogForStreamIfFound(streamCoords, true);
        if (catalog != null) {
            return catalog;
        }
        catalog = resolveExtensionCatalogForStreamIfFound(streamCoords, false);
        if (catalog != null) {
            return catalog;
        }
        throw unknownStreamException(streamCoords, false);
    }

    protected ExtensionCatalog resolveExtensionCatalogForStreamIfFound(PlatformStreamCoords streamCoords,
            boolean amongRecommended)
            throws RegistryResolutionException {
        for (RegistryExtensionResolver registry : registries) {
            final PlatformCatalog platforms = amongRecommended ? registry.resolvePlatformCatalog()
                    : registry.resolvePlatformCatalog(Constants.QUARKUS_VERSION_CLASSIFIER_ALL);
            if (platforms == null) {
                continue;
            }
            if (streamCoords.getPlatformKey() == null) {
                for (Platform p : platforms.getPlatforms()) {
                    var stream = p.getStream(streamCoords.getStreamId());
                    if (stream != null) {
                        return resolveExtensionCatalogForStream(stream, List.of(registry));
                    }
                }
            } else {
                final Platform platform = platforms.getPlatform(streamCoords.getPlatformKey());
                if (platform != null) {
                    var stream = platform.getStream(streamCoords.getStreamId());
                    if (stream != null) {
                        return resolveExtensionCatalogForStream(stream, List.of(registry));
                    }
                }
            }
        }
        return null;
    }

    private ExtensionCatalog resolveExtensionCatalogForStream(PlatformStream stream, List<RegistryExtensionResolver> registries)
            throws RegistryResolutionException {
        ExtensionCatalogBuilder catalogBuilder = new ExtensionCatalogBuilder();
        for (PlatformRelease release : stream.getReleases()) {
            collectExtensionCatalogs(release.getMemberBoms(), catalogBuilder, registries);
        }
        return catalogBuilder.build();
    }

    protected RegistryResolutionException unknownStreamException(PlatformStreamCoords stream, boolean amongRecommended)
            throws RegistryResolutionException {
        Platform requestedPlatform = null;
        final List<Platform> knownPlatforms = new ArrayList<>();
        for (RegistryExtensionResolver qer : registries) {
            final PlatformCatalog platforms = amongRecommended ? qer.resolvePlatformCatalog()
                    : qer.resolvePlatformCatalog(Constants.QUARKUS_VERSION_CLASSIFIER_ALL);
            if (platforms == null) {
                continue;
            }
            if (stream.getPlatformKey() != null) {
                requestedPlatform = platforms.getPlatform(stream.getPlatformKey());
                if (requestedPlatform != null) {
                    break;
                }
            }
            knownPlatforms.addAll(platforms.getPlatforms());
        }

        final StringBuilder buf = new StringBuilder();
        if (requestedPlatform != null) {
            buf.append("Failed to locate stream ").append(stream.getStreamId())
                    .append(" in platform ").append(requestedPlatform.getPlatformKey());
        } else if (knownPlatforms.isEmpty()) {
            buf.append("None of the registries provided any platform");
        } else {
            if (stream.getPlatformKey() == null) {
                buf.append("Failed to locate stream ").append(stream.getStreamId()).append(" in platform(s): ");
            } else {
                buf.append("Failed to locate platform ").append(stream.getPlatformKey())
                        .append(" among available platform(s): ");
            }
            buf.append(knownPlatforms.get(0).getPlatformKey());
            for (int i = 1; i < knownPlatforms.size(); ++i) {
                buf.append(", ").append(knownPlatforms.get(i).getPlatformKey());
            }
        }
        return new RegistryResolutionException(buf.toString());
    }

    public ExtensionCatalog resolveExtensionCatalog(Collection<ArtifactCoords> preferredPlatforms)
            throws RegistryResolutionException {
        if (preferredPlatforms.isEmpty()) {
            return resolveExtensionCatalog();
        }
        final ExtensionCatalogBuilder catalogBuilder = new ExtensionCatalogBuilder();
        collectExtensionCatalogs(preferredPlatforms, catalogBuilder, List.of());
        return catalogBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private void collectExtensionCatalogs(Collection<ArtifactCoords> preferredPlatforms, ExtensionCatalogBuilder catalogBuilder,
            List<RegistryExtensionResolver> registries)
            throws RegistryResolutionException {
        final Set<String> preferredPlatformKeys = new HashSet<>(4);
        final Set<ArtifactCoords> addedPlatformBoms = new HashSet<>();
        String quarkusVersion = null;
        for (ArtifactCoords bom : preferredPlatforms) {
            if (!addedPlatformBoms.add(bom)) {
                continue;
            }
            if (registries == null || registries.isEmpty()) {
                try {
                    registries = filterRegistries(r -> r.checkPlatform(bom));
                } catch (ExclusiveProviderConflictException e) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(
                            "The following registries were configured as exclusive providers of the ");
                    buf.append(PlatformArtifacts.ensureBomArtifact(bom).toCompactCoords());
                    buf.append(" platform: ").append(e.conflictingRegistries.get(0).getId());
                    for (int i = 1; i < e.conflictingRegistries.size(); ++i) {
                        buf.append(", ").append(e.conflictingRegistries.get(i).getId());
                    }
                    throw new RegistryResolutionException(buf.toString());
                }

                if (registries.isEmpty()) {
                    log.warn("None of the configured registries recognizes platform " + bom.toCompactCoords());
                    continue;
                }
            }

            ExtensionCatalog.Mutable catalog = null;
            RegistryExtensionResolver registry = null;
            int registryPreferenceIndex = 0;
            for (int i = 0; i < registries.size(); ++i) {
                registry = registries.get(i);
                try {
                    catalog = registry.resolvePlatformExtensions(bom);
                    registryPreferenceIndex = catalogBuilder.getRegistryPreferenceIndex(registry.getId());
                    break;
                } catch (RegistryResolutionException e) {
                    if (registries.size() == i + 1) {
                        throw e;
                    }
                    log.debug("%s", e.getLocalizedMessage());
                }
            }

            if (catalog == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Failed to resolve extension catalog of ")
                        .append(PlatformArtifacts.ensureBomArtifact(bom).toCompactCoords())
                        .append(" from ");
                buf.append(registries.get(0).getId());
                for (int i = 1; i < registries.size(); ++i) {
                    buf.append(", ").append(registries.get(i));
                }
                log.warn(buf.toString());
                throw new RegistryResolutionException(buf.toString());
            }

            if (quarkusVersion == null) {
                quarkusVersion = catalog.getQuarkusCoreVersion();
            }

            Map<String, Object> md = catalog.getMetadata();
            if (md != null) {
                Object o = md.get("platform-release");
                if (o instanceof Map platformRelease) {
                    o = platformRelease.get("platform-key");
                    if (o instanceof String platformKey) {
                        if (!preferredPlatformKeys.add(platformKey)) {
                            continue;
                        }
                        final Platform.Mutable platform = Platform.builder().setPlatformKey(platformKey);

                        final PlatformStream.Mutable stream = PlatformStream.builder()
                                .setId(String.valueOf(platformRelease.getOrDefault("stream", "default")));
                        platform.addStream(stream);

                        final PlatformRelease.Mutable release = PlatformRelease.builder()
                                .setVersion(PlatformReleaseVersion
                                        .fromString(String.valueOf(platformRelease.getOrDefault("version", "default"))))
                                .setQuarkusCoreVersion(catalog.getQuarkusCoreVersion())
                                .setUpstreamQuarkusCoreVersion(catalog.getUpstreamQuarkusCoreVersion());
                        stream.addRelease(release);

                        o = platformRelease.get("members");
                        if (o != null) {
                            final Collection<String> col = (Collection<String>) o;
                            final List<ArtifactCoords> memberCatalogs = new ArrayList<>(col.size());
                            for (String s : col) {
                                var memberCatalogCoords = ArtifactCoords.fromString(s);
                                memberCatalogs.add(memberCatalogCoords);
                                addedPlatformBoms.add(PlatformArtifacts.getBomArtifactForCatalog(memberCatalogCoords));
                            }
                            release.setMemberBoms(memberCatalogs);
                        }

                        collectPlatformExtensions(catalogBuilder, registry, platform);
                        continue;
                    }
                }
            }
            final int platformIndex = catalogBuilder.getPlatformPreferenceIndex(registryPreferenceIndex, bom.getGroupId())
                    .getPlatformIndex();
            final OriginPreference originPreference = new OriginPreference(registryPreferenceIndex, platformIndex, 1, 1,
                    catalogBuilder.getCompatibilityCode(quarkusVersion));

            addOriginPreference(catalog, originPreference);
            catalogBuilder.addCatalog(catalog);
        }

        if (quarkusVersion == null) {
            if (catalogBuilder.catalogs.isEmpty()) {
                final StringBuilder buf = new StringBuilder();
                final Iterator<ArtifactCoords> boms = preferredPlatforms.iterator();
                buf.append(PlatformArtifacts.ensureBomArtifact(boms.next()).toCompactCoords());
                while (boms.hasNext()) {
                    buf.append(", ").append(PlatformArtifacts.ensureBomArtifact(boms.next()).toCompactCoords());
                }
                buf.append(" could not be resolved from ");
                RegistryExtensionResolver registry = registries.get(0);
                buf.append(registry.getId());
                for (int i = 1; i < registries.size(); ++i) {
                    buf.append(", ").append(registries.get(i).getId());
                }
                throw new RegistryResolutionException(buf.toString());
            }
        }
        catalogBuilder.addUpstreamExtensionCatalogs(quarkusVersion, preferredPlatformKeys);
    }

    public void clearRegistryCache() throws RegistryResolutionException {
        for (RegistryExtensionResolver registry : registries) {
            registry.clearCache();
        }
    }

    private void ensureRegistriesConfigured() throws RegistryResolutionException {
        final int registriesTotal = registries.size();
        if (registriesTotal == 0) {
            throw new RegistryResolutionException("No registries configured");
        }
    }

    private static void appendNonPlatformExtensions(
            ExtensionCatalogBuilder catalogBuilder,
            String quarkusVersion) throws RegistryResolutionException {
        for (RegistryExtensionResolver registry : catalogBuilder.getRegistriesForQuarkusCore(quarkusVersion)) {
            appendNonPlatformExtensions(registry, catalogBuilder, quarkusVersion);
        }
    }

    private static void appendNonPlatformExtensions(RegistryExtensionResolver registry, ExtensionCatalogBuilder catalogBuilder,
            String quarkusVersion) throws RegistryResolutionException {
        final ExtensionCatalog.Mutable nonPlatformCatalog = registry.resolveNonPlatformExtensions(quarkusVersion);
        if (nonPlatformCatalog == null) {
            return;
        }
        final int compatibilityCode = catalogBuilder.getCompatibilityCode(quarkusVersion);
        final OriginPreference originPreference = new OriginPreference(
                catalogBuilder.getRegistryPreferenceIndex(registry.getId()),
                Integer.MAX_VALUE,
                compatibilityCode, 0, compatibilityCode);

        addOriginPreference(nonPlatformCatalog, originPreference);
        catalogBuilder.addCatalog(nonPlatformCatalog);
    }

    private static void collectPlatformExtensions(String quarkusCoreVersion, ExtensionCatalogBuilder catalogBuilder,
            Set<String> processedPlatformKeys)
            throws RegistryResolutionException {
        final List<RegistryExtensionResolver> quarkusVersionRegistries = catalogBuilder
                .getRegistriesForQuarkusCore(quarkusCoreVersion);

        for (RegistryExtensionResolver registry : quarkusVersionRegistries) {
            final PlatformCatalog platformCatalog = registry.resolvePlatformCatalog(quarkusCoreVersion);
            if (platformCatalog == null) {
                continue;
            }
            final Collection<Platform> platforms = platformCatalog.getPlatforms();
            if (platforms.isEmpty()) {
                continue;
            }
            for (Platform p : platforms) {
                if (processedPlatformKeys.contains(p.getPlatformKey())) {
                    continue;
                }
                collectPlatformExtensions(catalogBuilder, registry, p);
            }
        }
    }

    private static void collectPlatformExtensions(ExtensionCatalogBuilder catalogBuilder,
            RegistryExtensionResolver registry, Platform platform) throws RegistryResolutionException {

        final int registryPreferenceIndex = catalogBuilder.getRegistryPreferenceIndex(registry.getId());
        final PlatformReleasePreferenceIndex releasePreference = catalogBuilder
                .getPlatformPreferenceIndex(registryPreferenceIndex, platform.getPlatformKey());
        for (PlatformStream s : platform.getStreams()) {
            for (PlatformRelease r : s.getReleases()) {
                final int releaseIndex = releasePreference.getReleaseIndex(r.getVersion().toString());
                int memberIndex = 0;
                final int compatibilityCode = catalogBuilder.getCompatibilityCode(r.getQuarkusCoreVersion(),
                        r.getUpstreamQuarkusCoreVersion());

                for (ArtifactCoords bom : r.getMemberBoms()) {
                    final ExtensionCatalog.Mutable catalog = registry.resolvePlatformExtensions(bom);

                    if (catalog != null) {
                        final OriginPreference originPreference = new OriginPreference(registryPreferenceIndex,
                                releasePreference.getPlatformIndex(),
                                releaseIndex, ++memberIndex, compatibilityCode);
                        addOriginPreference(catalog, originPreference);

                        catalogBuilder.addCatalog(catalog);
                    }
                }

                final String upstreamQuarkusVersion = r.getUpstreamQuarkusCoreVersion();
                if (upstreamQuarkusVersion != null) {
                    catalogBuilder.addUpstreamQuarkusVersion(upstreamQuarkusVersion);
                }
            }
        }
    }

    private List<RegistryExtensionResolver> getRegistriesForQuarkusVersion(String quarkusCoreVersion) {
        try {
            return filterRegistries(r -> r.checkQuarkusVersion(quarkusCoreVersion));
        } catch (ExclusiveProviderConflictException e) {
            final StringBuilder buf = new StringBuilder();
            buf.append(
                    "The following registries were configured as exclusive providers of extensions based on Quarkus version ");
            buf.append(quarkusCoreVersion);
            buf.append(": ").append(e.conflictingRegistries.get(0).getId());
            for (int i = 1; i < e.conflictingRegistries.size(); ++i) {
                buf.append(", ").append(e.conflictingRegistries.get(i).getId());
            }
            throw new RuntimeException(buf.toString());
        }
    }

    private List<RegistryExtensionResolver> filterRegistries(Function<RegistryExtensionResolver, Integer> recognizer)
            throws ExclusiveProviderConflictException {
        RegistryExtensionResolver exclusiveProvider = null;
        List<RegistryExtensionResolver> filtered = null;
        List<RegistryExtensionResolver> conflicts = null;
        for (int i = 0; i < registries.size(); ++i) {
            final RegistryExtensionResolver registry = registries.get(i);
            final int versionCheck = recognizer.apply(registry);

            if (versionCheck == RegistryExtensionResolver.VERSION_NOT_RECOGNIZED) {
                if (exclusiveProvider == null && filtered == null) {
                    filtered = new ArrayList<>(registries.size() - 1);
                    for (int j = 0; j < i; ++j) {
                        filtered.add(registries.get(j));
                    }
                }
                continue;
            }

            if (versionCheck == RegistryExtensionResolver.VERSION_EXCLUSIVE_PROVIDER) {
                if (exclusiveProvider == null) {
                    exclusiveProvider = registry;
                } else {
                    if (conflicts == null) {
                        conflicts = new ArrayList<>();
                        conflicts.add(exclusiveProvider);
                    }
                    conflicts.add(registry);
                }
            }

            if (filtered != null) {
                filtered.add(registry);
            }
        }

        if (conflicts != null) {
            throw new ExclusiveProviderConflictException(conflicts);
        }

        return exclusiveProvider == null ? filtered == null ? registries : filtered : List.of(exclusiveProvider);
    }
}
