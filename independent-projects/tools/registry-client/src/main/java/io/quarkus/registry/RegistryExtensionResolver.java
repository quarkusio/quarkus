package io.quarkus.registry;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.client.RegistryClient;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.util.GlobUtil;
import java.util.Objects;
import java.util.regex.Pattern;

class RegistryExtensionResolver {

    public static final int VERSION_NOT_RECOGNIZED = -1;
    public static final int VERSION_NOT_CONFIGURED = 0;
    public static final int VERSION_RECOGNIZED = 1;
    public static final int VERSION_EXCLUSIVE_PROVIDER = 2;

    private final RegistryConfig config;
    private final RegistryClient extensionResolver;
    private final int index;

    private Pattern recognizedQuarkusVersions;

    RegistryExtensionResolver(RegistryClient extensionResolver,
            MessageWriter log, int index) throws RegistryResolutionException {
        this.extensionResolver = Objects.requireNonNull(extensionResolver, "Registry extension resolver is null");
        this.config = extensionResolver.resolveRegistryConfig();
        this.index = index;

        String versionExpr = config.getQuarkusVersions() == null ? null
                : config.getQuarkusVersions().getRecognizedVersionsExpression();
        if (versionExpr != null) {
            recognizedQuarkusVersions = Pattern.compile(GlobUtil.toRegexPattern(versionExpr));
        }
    }

    String getId() {
        return config.getId();
    }

    int getIndex() {
        return index;
    }

    int checkQuarkusVersion(String quarkusVersion) {
        if (recognizedQuarkusVersions == null) {
            return VERSION_NOT_CONFIGURED;
        }
        if (!recognizedQuarkusVersions.matcher(quarkusVersion).matches()) {
            return VERSION_NOT_RECOGNIZED;
        }
        return config.getQuarkusVersions().isExclusiveProvider() ? VERSION_EXCLUSIVE_PROVIDER
                : VERSION_RECOGNIZED;
    }

    int checkPlatform(ArtifactCoords platform) {
        // TODO this should be allowed to check the full coordinates
        return checkQuarkusVersion(platform.getVersion());
    }

    PlatformCatalog resolvePlatformCatalog() throws RegistryResolutionException {
        return resolvePlatformCatalog(null);
    }

    PlatformCatalog resolvePlatformCatalog(String quarkusCoreVersion) throws RegistryResolutionException {
        return extensionResolver.resolvePlatforms(quarkusCoreVersion);
    }

    Platform resolveRecommendedPlatform() throws RegistryResolutionException {
        return resolvePlatformCatalog().getRecommendedPlatform();
    }

    ExtensionCatalog resolveNonPlatformExtensions(String quarkusCoreVersion) throws RegistryResolutionException {
        return extensionResolver.resolveNonPlatformExtensions(quarkusCoreVersion);
    }

    ExtensionCatalog resolvePlatformExtensions(ArtifactCoords platform) throws RegistryResolutionException {
        return extensionResolver.resolvePlatformExtensions(platform);
    }

    void clearCache() throws RegistryResolutionException {
        extensionResolver.clearCache();
    }
}
