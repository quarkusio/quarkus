package io.quarkus.registry;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformRelease;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.catalog.PlatformStreamCoords;
import io.quarkus.registry.catalog.json.JsonCatalogMerger;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import io.quarkus.registry.catalog.selection.OriginPreference;
import io.quarkus.registry.client.RegistryClientFactory;
import io.quarkus.registry.client.maven.MavenRegistryClientFactory;
import io.quarkus.registry.client.spi.RegistryClientEnvironment;
import io.quarkus.registry.client.spi.RegistryClientFactoryProvider;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ExtensionCatalogResolver {

    public static ExtensionCatalogResolver empty() {
        final ExtensionCatalogResolver resolver = new ExtensionCatalogResolver();
        resolver.registries = Collections.emptyList();
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
                        .add(new RegistryExtensionResolver(clientFactory.buildRegistryClient(config), log, registries.size()));
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
            ArtifactCoords providerArtifact = null;
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
            final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
            try {
                ClassLoader providerCl = new URLClassLoader(new URL[] { url }, originalCl);
                final Iterator<RegistryClientFactoryProvider> i = ServiceLoader
                        .load(RegistryClientFactoryProvider.class, providerCl).iterator();
                if (!i.hasNext()) {
                    throw new Exception("Failed to locate an implementation of " + RegistryClientFactoryProvider.class.getName()
                            + " service provider");
                }
                final RegistryClientFactoryProvider provider = i.next();
                if (i.hasNext()) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append("Found more than one registry client factory provider "
                            + provider.getClass().getName());
                    while (i.hasNext()) {
                        buf.append(", ").append(i.next().getClass().getName());
                    }
                    throw new Exception(buf.toString());
                }
                return provider.newRegistryClientFactory(getClientEnv());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load registry client factory from " + url, e);
            } finally {
                Thread.currentThread().setContextClassLoader(originalCl);
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

        final JsonPlatformCatalog result = new JsonPlatformCatalog();

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

        if (lastUpdated != null) {
            result.getMetadata().put(Constants.LAST_UPDATED, lastUpdated);
        }
        result.setPlatforms(collectedPlatforms);
        return result;
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
        return registries.get(getRegistryIndex(registryId)).resolvePlatformCatalog();
    }

    public PlatformCatalog resolvePlatformCatalogFromRegistry(String registryId, String quarkusVersion)
            throws RegistryResolutionException {
        return quarkusVersion == null ? resolvePlatformCatalogFromRegistry(registryId)
                : registries.get(getRegistryIndex(registryId)).resolvePlatformCatalog(quarkusVersion);
    }

    private class ExtensionCatalogBuilder {
        private final List<ExtensionCatalog> catalogs = new ArrayList<>();
        final Map<String, List<RegistryExtensionResolver>> registriesByQuarkusCore = new HashMap<>();
        private final Map<String, Integer> compatibilityCodes = new LinkedHashMap<>();
        final List<String> upstreamQuarkusVersions = new ArrayList<>(1);

        void addCatalog(ExtensionCatalog c) {
            catalogs.add(c);
        }

        void addUpstreamQuarkusVersion(String quarkusVersion) {
            if (!upstreamQuarkusVersions.contains(quarkusVersion)) {
                upstreamQuarkusVersions.add(quarkusVersion);
            }
        }

        List<RegistryExtensionResolver> getRegistriesForQuarkusCore(String quarkusVersion) {
            return registriesByQuarkusCore.computeIfAbsent(quarkusVersion, v -> getRegistriesForQuarkusVersion(v));
        }

        public int getCompatibilityCode(String quarkusVersion) {
            return getCompatibilityCode(quarkusVersion, null);
        }

        public int getCompatibilityCode(String quarkusVersion, String upstreamQuarkusVersion) {
            Integer i = compatibilityCodes.get(quarkusVersion);
            if (i == null) {
                if (upstreamQuarkusVersion != null) {
                    i = compatibilityCodes.get(upstreamQuarkusVersion);
                    if (i == null) {
                        i = compatibilityCodes.size();
                        compatibilityCodes.put(upstreamQuarkusVersion, i);
                    }
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

        ExtensionCatalog build() throws RegistryResolutionException {
            appendAllNonPlatformExtensions();
            return JsonCatalogMerger.merge(catalogs);
        }
    }

    public ExtensionCatalog resolveExtensionCatalog() throws RegistryResolutionException {

        ensureRegistriesConfigured();

        final ExtensionCatalogBuilder catalogBuilder = new ExtensionCatalogBuilder();

        for (int registryIndex = 0; registryIndex < registries.size(); ++registryIndex) {
            collectPlatformExtensions(catalogBuilder, registryIndex);
        }

        return catalogBuilder.build();
    }

    private void collectPlatformExtensions(final ExtensionCatalogBuilder catalogBuilder, int registryIndex)
            throws RegistryResolutionException {
        final RegistryExtensionResolver registry = registries.get(registryIndex);

        final List<PlatformCatalog> downstreamPreferences = new ArrayList<>(catalogBuilder.upstreamQuarkusVersions.size());
        for (String quarkusVersion : catalogBuilder.upstreamQuarkusVersions) {
            if (!registry.isAcceptsQuarkusVersionQueries(quarkusVersion)) {
                continue;
            }
            final PlatformCatalog pc = registry.resolvePlatformCatalog(quarkusVersion);
            if (pc == null) {
                continue;
            }
            downstreamPreferences.add(pc);
        }

        PlatformCatalog pc = registry.resolvePlatformCatalog();
        if (pc == null && downstreamPreferences.isEmpty()) {
            return;
        }
        if (!downstreamPreferences.isEmpty()) {
            downstreamPreferences.add(pc);
            pc = JsonCatalogMerger.mergePlatformCatalogs(downstreamPreferences);
        }

        int platformIndex = 0;
        for (Platform platform : pc.getPlatforms()) {
            platformIndex++;
            for (PlatformStream stream : platform.getStreams()) {
                int releaseIndex = 0;
                for (PlatformRelease release : stream.getReleases()) {
                    releaseIndex++;
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
                        final ExtensionCatalog ec = registry.resolvePlatformExtensions(bom);
                        if (ec != null) {
                            final OriginPreference originPreference = new OriginPreference(registryIndex, platformIndex,
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

    private void addOriginPreference(final ExtensionCatalog ec, OriginPreference originPreference) {
        Map<String, Object> metadata = ec.getMetadata();
        if (metadata.isEmpty()) {
            metadata = new HashMap<>(1);
            ((JsonExtensionCatalog) ec).setMetadata(metadata);
        }
        metadata.put("origin-preference", originPreference);
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
            final ExtensionCatalogBuilder catalogBuilder, Set<String> preferredPlatforms)
            throws RegistryResolutionException {
        collectPlatformExtensions(quarkusCoreVersion, catalogBuilder, preferredPlatforms);
        int i = 0;
        while (i < catalogBuilder.upstreamQuarkusVersions.size()) {
            collectPlatformExtensions(catalogBuilder.upstreamQuarkusVersions.get(i++), catalogBuilder, preferredPlatforms);
        }
        return catalogBuilder.build();
    }

    public ExtensionCatalog resolveExtensionCatalog(PlatformStreamCoords streamCoords) throws RegistryResolutionException {

        ensureRegistriesConfigured();

        final String platformKey = streamCoords.getPlatformKey();
        final String streamId = streamCoords.getStreamId();

        PlatformStream stream = null;
        int registryIndex = 0;
        while (registryIndex < registries.size()) {
            final RegistryExtensionResolver qer = registries.get(registryIndex++);
            final PlatformCatalog platforms = qer.resolvePlatformCatalog();
            if (platforms == null) {
                continue;
            }
            if (platformKey == null) {
                for (Platform p : platforms.getPlatforms()) {
                    stream = p.getStream(streamId);
                    if (stream != null) {
                        break;
                    }
                }
            } else {
                final Platform platform = platforms.getPlatform(platformKey);
                if (platform == null) {
                    continue;
                }
                stream = platform.getStream(streamId);
            }
            break;
        }

        if (stream == null) {
            Platform requestedPlatform = null;
            final List<Platform> knownPlatforms = new ArrayList<>();
            for (RegistryExtensionResolver qer : registries) {
                final PlatformCatalog platforms = qer.resolvePlatformCatalog();
                if (platforms == null) {
                    continue;
                }
                if (platformKey != null) {
                    requestedPlatform = platforms.getPlatform(platformKey);
                    if (requestedPlatform != null) {
                        break;
                    }
                }
                for (Platform platform : platforms.getPlatforms()) {
                    knownPlatforms.add(platform);
                }
            }

            final StringBuilder buf = new StringBuilder();
            if (requestedPlatform != null) {
                buf.append("Failed to locate stream ").append(streamId)
                        .append(" in platform " + requestedPlatform.getPlatformKey());
            } else if (knownPlatforms.isEmpty()) {
                buf.append("None of the registries provided any platform");
            } else {
                if (platformKey == null) {
                    buf.append("Failed to locate stream ").append(streamId).append(" in platform(s): ");
                } else {
                    buf.append("Failed to locate platform ").append(platformKey).append(" among available platform(s): ");
                }
                buf.append(knownPlatforms.get(0).getPlatformKey());
                for (int i = 1; i < knownPlatforms.size(); ++i) {
                    buf.append(", ").append(knownPlatforms.get(i).getPlatformKey());
                }
            }
            throw new RegistryResolutionException(buf.toString());
        }

        final List<ExtensionCatalog> catalogs = new ArrayList<>();
        for (PlatformRelease release : stream.getReleases()) {
            catalogs.add(resolveExtensionCatalog(release.getMemberBoms()));
        }

        return JsonCatalogMerger.merge(catalogs);
    }

    @SuppressWarnings("unchecked")
    public ExtensionCatalog resolveExtensionCatalog(Collection<ArtifactCoords> preferredPlatforms)
            throws RegistryResolutionException {
        if (preferredPlatforms.isEmpty()) {
            return resolveExtensionCatalog();
        }

        final ExtensionCatalogBuilder catalogBuilder = new ExtensionCatalogBuilder();
        final Set<String> preferredPlatformKeys = new HashSet<>();
        String quarkusVersion = null;
        int platformIndex = 0;
        for (ArtifactCoords bom : preferredPlatforms) {
            final List<RegistryExtensionResolver> registries;
            try {
                registries = filterRegistries(r -> r.checkPlatform(bom));
            } catch (ExclusiveProviderConflictException e) {
                final StringBuilder buf = new StringBuilder();
                buf.append(
                        "The following registries were configured as exclusive providers of the ");
                buf.append(bom);
                buf.append("platform: ").append(e.conflictingRegistries.get(0).getId());
                for (int i = 1; i < e.conflictingRegistries.size(); ++i) {
                    buf.append(", ").append(e.conflictingRegistries.get(i).getId());
                }
                throw new RegistryResolutionException(buf.toString());
            }

            if (registries.isEmpty()) {
                log.warn("None of the configured registries recognizes platform %s", bom);
                continue;
            }

            ExtensionCatalog catalog = null;
            RegistryExtensionResolver registry = null;
            for (int i = 0; i < registries.size(); ++i) {
                registry = registries.get(i);
                try {
                    catalog = registry.resolvePlatformExtensions(bom);
                    break;
                } catch (RegistryResolutionException e) {
                }
            }

            if (catalog == null) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Failed to resolve platform ").append(bom).append(" using the following registries: ");
                buf.append(registries.get(0).getId());
                for (int i = 1; i < registries.size(); ++i) {
                    buf.append(", ").append(registries.get(i++));
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
                if (o != null && o instanceof Map) {
                    md = (Map<String, Object>) o;
                    o = md.get("platform-key");
                    if (o != null && o instanceof String) {
                        final String platformKey = o.toString();
                        if (!preferredPlatformKeys.add(platformKey)) {
                            continue;
                        }
                        final JsonPlatform p = new JsonPlatform();
                        p.setPlatformKey(platformKey);

                        final JsonPlatformStream stream = new JsonPlatformStream();
                        stream.setId(String.valueOf(md.getOrDefault("stream", "default")));
                        p.addStream(stream);

                        final JsonPlatformRelease release = new JsonPlatformRelease();
                        release.setVersion(
                                JsonPlatformReleaseVersion.fromString(String.valueOf(md.getOrDefault("version", "default"))));
                        release.setQuarkusCoreVersion(catalog.getQuarkusCoreVersion());
                        release.setUpstreamQuarkusCoreVersion(catalog.getUpstreamQuarkusCoreVersion());
                        stream.addRelease(release);

                        o = md.get("members");
                        if (o != null) {
                            final Collection<String> col = (Collection<String>) o;
                            final List<ArtifactCoords> coords = new ArrayList<>(col.size());
                            for (String s : col) {
                                coords.add(ArtifactCoords.fromString(s));
                            }
                            release.setMemberBoms(coords);
                        }

                        collectPlatformExtensions(quarkusVersion, catalogBuilder, registry, platformIndex,
                                p);
                        continue;
                    }
                }
            }
            final OriginPreference originPreference = new OriginPreference(0, ++platformIndex, 1, 1,
                    catalogBuilder.getCompatibilityCode(quarkusVersion));
            addOriginPreference(catalog, originPreference);
            catalogBuilder.addCatalog(catalog);
        }

        return preferredPlatforms.isEmpty() ? catalogBuilder.build()
                : resolveExtensionCatalog(quarkusVersion, catalogBuilder, preferredPlatformKeys);
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

    private void appendNonPlatformExtensions(
            ExtensionCatalogBuilder catalogBuilder,
            String quarkusVersion) throws RegistryResolutionException {
        for (RegistryExtensionResolver registry : catalogBuilder.getRegistriesForQuarkusCore(quarkusVersion)) {
            appendNonPlatformExtensions(registry, catalogBuilder, quarkusVersion);
        }
    }

    public void appendNonPlatformExtensions(RegistryExtensionResolver registry, ExtensionCatalogBuilder catalogBuilder,
            String quarkusVersion) throws RegistryResolutionException {
        final ExtensionCatalog nonPlatformCatalog = registry.resolveNonPlatformExtensions(quarkusVersion);
        if (nonPlatformCatalog == null) {
            return;
        }
        final int compatibilityCode = catalogBuilder.getCompatibilityCode(quarkusVersion);
        final OriginPreference originPreference = new OriginPreference(registry.getIndex(),
                Integer.MAX_VALUE,
                compatibilityCode, 0, compatibilityCode);
        addOriginPreference(nonPlatformCatalog, originPreference);
        catalogBuilder.addCatalog(nonPlatformCatalog);
    }

    private int getRegistryIndex(String registryId) {
        int registryIndex = 0;
        while (registryIndex < registries.size()) {
            if (registries.get(registryIndex).getId().equals(registryId)) {
                return registryIndex;
            }
            ++registryIndex;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("Failed to locate ").append(registryId).append(" among the configured registries:");
        registries.forEach(r -> buf.append(" ").append(r.getId()));
        throw new IllegalStateException(buf.toString());
    }

    private void collectPlatformExtensions(String quarkusCoreVersion, ExtensionCatalogBuilder catalogBuilder,
            Set<String> processedPlatforms)
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
            int platformIndex = processedPlatforms.size();
            for (Platform p : platforms) {
                if (processedPlatforms.contains(p.getPlatformKey())) {
                    continue;
                }
                ++platformIndex;
                collectPlatformExtensions(quarkusCoreVersion, catalogBuilder, registry, platformIndex, p);
            }
        }
    }

    private void collectPlatformExtensions(String quarkusCoreVersion, ExtensionCatalogBuilder catalogBuilder,
            RegistryExtensionResolver registry, int platformIndex,
            Platform p) throws RegistryResolutionException {
        for (PlatformStream s : p.getStreams()) {
            int releaseIndex = 0;
            for (PlatformRelease r : s.getReleases()) {
                ++releaseIndex;
                int memberIndex = 0;
                final int compatibilityCode = catalogBuilder.getCompatibilityCode(r.getQuarkusCoreVersion(),
                        r.getUpstreamQuarkusCoreVersion());
                for (ArtifactCoords bom : r.getMemberBoms()) {
                    final ExtensionCatalog catalog = registry.resolvePlatformExtensions(bom);
                    if (catalog != null) {

                        final OriginPreference originPreference = new OriginPreference(registry.getIndex(),
                                platformIndex,
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

        return exclusiveProvider == null ? filtered == null ? registries : filtered : Arrays.asList(exclusiveProvider);
    }
}
