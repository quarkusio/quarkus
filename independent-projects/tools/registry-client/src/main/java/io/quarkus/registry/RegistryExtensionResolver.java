package io.quarkus.registry;

import static io.quarkus.registry.Constants.OFFERING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
     * Returns offering configured by the user in the registry configuration or null, in case
     * no offering was configured.
     *
     * An offering would be the name part of the {@code <name>-support} in the extension metadata.
     *
     * @param config registry configuration
     * @return user configured offering or null
     */
    private static String getConfiguredOfferingOrNull(RegistryConfig config) {
        var offering = config.getExtra().get(OFFERING);
        if (offering == null) {
            return null;
        }
        if (!(offering instanceof String)) {
            throw new IllegalArgumentException("Expected a string value for option " + OFFERING + " but got " + offering
                    + " of type " + offering.getClass().getName());
        }
        final String str = offering.toString();
        return str.isBlank() ? null : str;
    }

    private final RegistryConfig config;
    private final RegistryClient extensionResolver;

    private final Pattern recognizedQuarkusVersions;
    private final Collection<String> recognizedGroupIds;
    /**
     * {@code -support} extension metadata key that corresponds to the user selected offering, can be null
     */
    private final String offeringSupportKey;

    RegistryExtensionResolver(RegistryClient registryClient, MessageWriter log) throws RegistryResolutionException {
        this.extensionResolver = Objects.requireNonNull(registryClient, "Registry extension resolver is null");
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
        return extensionResolver.resolvePlatforms(quarkusCoreVersion);
    }

    Platform resolveRecommendedPlatform() throws RegistryResolutionException {
        return resolvePlatformCatalog().getRecommendedPlatform();
    }

    ExtensionCatalog.Mutable resolveNonPlatformExtensions(String quarkusCoreVersion) throws RegistryResolutionException {
        return applyOfferingFilter(extensionResolver.resolveNonPlatformExtensions(quarkusCoreVersion));
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
        return applyOfferingFilter(extensionResolver.resolvePlatformExtensions(platform));
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
        extensionResolver.clearCache();
    }
}
