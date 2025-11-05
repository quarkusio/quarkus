package io.quarkus.registry;

import static io.quarkus.registry.Constants.OFFERING;
import static io.quarkus.registry.Constants.RECOMMEND_STREAMS_FROM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.util.GlobUtil;

class RegistryExtensionResolver {

    public static final int VERSION_NOT_RECOGNIZED = -1;
    public static final int VERSION_NOT_CONFIGURED = 0;
    public static final int VERSION_RECOGNIZED = 1;
    public static final int VERSION_EXCLUSIVE_PROVIDER = 2;

    private static final String DASH_SUPPORT = "-support";

    /**
     * Returns an extra config option value with an expected type.
     * If an option was not configured, the returned value will be null.
     * If the configured value cannot be cast to an expected type, the method will throw an error.
     *
     * @param config registry configuration
     * @param name option name
     * @param type expected value type
     * @return configured value or null
     * @param <T> expected value type
     */
    private static <T> T getExtraConfigOption(RegistryConfig config, String name, Class<T> type) {
        Object o = config.getExtra().get(name);
        if (o == null) {
            return null;
        }
        if (type.isInstance(o)) {
            return (T) o;
        }
        throw new IllegalArgumentException(
                "Expected a value of type " + type.getName() + " for option " + name + " but got " + o
                        + " of type " + o.getClass().getName());
    }

    /**
     * Returns offering configured by the user in the registry configuration or null, in case
     * no offering was configured.
     *
     * An offering would be the name part of the {@code <name>-support} in the extension metadata.
     *
     * @param config registry configuration
     * @return user configured offering or null
     */
    private static String getConfiguredOfferingOrNull(RegistryConfig config) {
        var offering = getExtraConfigOption(config, OFFERING, String.class);
        return offering == null || offering.isBlank() ? null : offering;
    }

    /**
     * Returns the lowest boundaries for recommended streams, if configured.
     * The returned map will have platform keys as the map keys and stream IDs split using dot as a separator.
     * If the lowest boundaries have not been configured, this method will return an empty map.
     *
     * @param config registry configuration
     * @return lowest boundaries for recommended streams per platform key or an empty map, if not configured
     */
    private static Map<String, String[]> getRecommendStreamsFromOrNull(RegistryConfig config) {
        final Map<String, String> recommendStreamsStarting = getExtraConfigOption(config, RECOMMEND_STREAMS_FROM, Map.class);
        if (recommendStreamsStarting == null) {
            return Map.of();
        }
        final Map<String, String[]> result = new HashMap<>(recommendStreamsStarting.size());
        for (Map.Entry<String, String> e : recommendStreamsStarting.entrySet()) {
            result.put(e.getKey(), e.getValue().split("\\."));
        }
        return result;
    }

    /**
     * Compares two streams as versions.
     *
     * @param streamParts1 stream one
     * @param streamParts2 stream two
     * @return 1 if stream one is newer than stream two, -1 if stream two is newer than stream 1, otherwise 0
     */
    private static int compareStreams(String[] streamParts1, String[] streamParts2) {
        int partI = 0;
        while (true) {
            if (partI == streamParts1.length) {
                return partI == streamParts2.length ? 0 : -1;
            }
            if (partI == streamParts2.length) {
                return 1;
            }
            final int result = streamParts1[partI].compareTo(streamParts2[partI]);
            if (result != 0) {
                return result;
            }
            ++partI;
        }
    }

    private final RegistryConfig config;
    private final RegistryClient registry;

    private final Pattern recognizedQuarkusVersions;
    private final Collection<String> recognizedGroupIds;

    /**
     * {@code -support} extension metadata key that corresponds to the user selected offering, can be null
     */
    private final String offeringSupportKey;

    /**
     * If configured, this will be the lowest boundary for stream recommendations.
     * IOW, streams before this value will be ignored.
     */
    private final Map<String, String[]> recommendStreamsFrom;

    RegistryExtensionResolver(RegistryClient registryClient, MessageWriter log) throws RegistryResolutionException {
        this.registry = Objects.requireNonNull(registryClient, "Registry extension resolver is null");
        this.config = registryClient.resolveRegistryConfig();

        final String versionExpr = config.getQuarkusVersions() == null ? null
                : config.getQuarkusVersions().getRecognizedVersionsExpression();
        recognizedQuarkusVersions = versionExpr == null ? null : Pattern.compile(GlobUtil.toRegexPattern(versionExpr));
        this.recognizedGroupIds = config.getQuarkusVersions() == null ? Collections.emptyList()
                : config.getQuarkusVersions().getRecognizedGroupIds();

        final String offering = getConfiguredOfferingOrNull(config);
        if (offering != null) {
            log.debug("Registry %s is limited to offerings %s", config.getId(), offering);
            this.offeringSupportKey = offering + DASH_SUPPORT;
        } else {
            offeringSupportKey = null;
        }

        recommendStreamsFrom = getRecommendStreamsFromOrNull(config);
        if (!recommendStreamsFrom.isEmpty()) {
            log.debug("Streams before %s recommended by %s will be ignored", recommendStreamsFrom, config.getId());
        }
    }

    String getId() {
        return config.getId();
    }

    int checkQuarkusVersion(String quarkusVersion) {
        if (recognizedQuarkusVersions == null) {
            return VERSION_NOT_CONFIGURED;
        }
        if (quarkusVersion == null) {
            throw new IllegalArgumentException();
        }
        if (!recognizedQuarkusVersions.matcher(quarkusVersion).matches()) {
            return VERSION_NOT_RECOGNIZED;
        }
        return config.getQuarkusVersions().isExclusiveProvider() ? VERSION_EXCLUSIVE_PROVIDER
                : VERSION_RECOGNIZED;
    }

    boolean isExclusiveProviderOf(String quarkusVersion) {
        return checkQuarkusVersion(quarkusVersion) == VERSION_EXCLUSIVE_PROVIDER;
    }

    boolean isAcceptsQuarkusVersionQueries(String quarkusVersion) {
        return checkQuarkusVersion(quarkusVersion) >= 0;
    }

    int checkPlatform(ArtifactCoords platform) {
        if (!recognizedGroupIds.isEmpty() && !recognizedGroupIds.contains(platform.getGroupId())) {
            return VERSION_NOT_RECOGNIZED;
        }
        return checkQuarkusVersion(platform.getVersion());
    }

    PlatformCatalog.Mutable resolvePlatformCatalog() throws RegistryResolutionException {
        return resolvePlatformCatalog(null);
    }

    PlatformCatalog.Mutable resolvePlatformCatalog(String quarkusCoreVersion) throws RegistryResolutionException {
        PlatformCatalog.Mutable platformCatalog = registry.resolvePlatforms(quarkusCoreVersion);
        if (!recommendStreamsFrom.isEmpty()) {
            final Iterator<Platform> platformI = platformCatalog.getPlatforms().iterator();
            boolean allPlatformsIgnored = true;
            while (platformI.hasNext()) {
                var platform = platformI.next();
                final String[] fromStream = recommendStreamsFrom.get(platform.getPlatformKey());
                if (fromStream != null) {
                    final Iterator<PlatformStream> streamI = platform.getStreams().iterator();
                    boolean allStreamsIgnored = true;
                    while (streamI.hasNext()) {
                        var stream = streamI.next();
                        if (compareStreams(fromStream, stream.getId().split("\\.")) > 0) {
                            streamI.remove();
                            break;
                        } else {
                            allStreamsIgnored = false;
                        }
                    }
                    while (streamI.hasNext()) {
                        streamI.next();
                        streamI.remove();
                    }
                    if (allStreamsIgnored) {
                        platformI.remove();
                    } else {
                        allPlatformsIgnored = false;
                    }
                } else {
                    allPlatformsIgnored = false;
                }
            }
            if (allPlatformsIgnored) {
                return null;
            }
        }
        return platformCatalog;
    }

    Platform resolveRecommendedPlatform() throws RegistryResolutionException {
        return resolvePlatformCatalog().getRecommendedPlatform();
    }

    ExtensionCatalog.Mutable resolveNonPlatformExtensions(String quarkusCoreVersion) throws RegistryResolutionException {
        return applyOfferingFilter(registry.resolveNonPlatformExtensions(quarkusCoreVersion));
    }

    /**
     * Resolves a platform extension catalog using a given JSON artifact.
     * <p>
     * If a user did not configure an offering the original catalog is returned.
     * <p>
     * If an offering was configured, we filter out extensions that are not part of the selected offering
     * {@link Constants#DEFAULT_REGISTRY_ARTIFACT_VERSION} to the metadata with the corresponding {@code <name>-support}
     * as its value that will be used by the extension list commands later to display the relevant support scope.
     *
     * @param platform either a BOM or a JSON descriptor coordinates
     * @return extension catalog
     * @throws RegistryResolutionException in case of a failure
     */
    ExtensionCatalog.Mutable resolvePlatformExtensions(ArtifactCoords platform) throws RegistryResolutionException {
        return applyOfferingFilter(registry.resolvePlatformExtensions(platform));
    }

    private ExtensionCatalog.Mutable applyOfferingFilter(ExtensionCatalog.Mutable catalog) {
        if (catalog == null) {
            return null;
        }
        final Collection<Extension> originalCollection = catalog.getExtensions();
        final List<Extension> offeringCollection = offeringSupportKey == null ? null
                : new ArrayList<>(originalCollection.size());
        final Map<ArtifactKey, ArtifactCoords> allCatalogExtensions = new HashMap<>(originalCollection.size());
        for (Extension ext : originalCollection) {
            allCatalogExtensions.put(ext.getArtifact().getKey(), ext.getArtifact());
            if (offeringCollection != null) {
                if (ext.getMetadata().containsKey(offeringSupportKey)) {
                    ext.getMetadata().put(Constants.REGISTRY_CLIENT_USER_SELECTED_SUPPORT_KEY, offeringSupportKey);
                    offeringCollection.add(ext);
                } else if (ext.getArtifact().getArtifactId().equals("quarkus-core")) {
                    // we need quarkus-core for proper quarkus-bom to become the primary platform when creating projects
                    offeringCollection.add(ext);
                }
            }
        }
        catalog.getMetadata().put(Constants.REGISTRY_CLIENT_ALL_CATALOG_EXTENSIONS, allCatalogExtensions);
        if (offeringCollection != null) {
            catalog.setExtensions(offeringCollection);
        }
        return catalog;
    }

    void clearCache() throws RegistryResolutionException {
        registry.clearCache();
    }
}
