package io.quarkus.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;
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

    private final RegistryConfig config;
    private final RegistryClient extensionResolver;
    private final int index;

    private final Pattern recognizedQuarkusVersions;
    private final Collection<String> recognizedGroupIds;

    RegistryExtensionResolver(RegistryClient extensionResolver,
            MessageWriter log, int index) throws RegistryResolutionException {
        this.extensionResolver = Objects.requireNonNull(extensionResolver, "Registry extension resolver is null");
        this.config = extensionResolver.resolveRegistryConfig();
        this.index = index;

        final String versionExpr = config.getQuarkusVersions() == null ? null
                : config.getQuarkusVersions().getRecognizedVersionsExpression();
        recognizedQuarkusVersions = versionExpr == null ? null : Pattern.compile(GlobUtil.toRegexPattern(versionExpr));
        this.recognizedGroupIds = config.getQuarkusVersions() == null ? Collections.emptyList()
                : config.getQuarkusVersions().getRecognizedGroupIds();
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
        return extensionResolver.resolveNonPlatformExtensions(quarkusCoreVersion);
    }

    ExtensionCatalog.Mutable resolvePlatformExtensions(ArtifactCoords platform) throws RegistryResolutionException {
        return extensionResolver.resolvePlatformExtensions(platform);
    }

    void clearCache() throws RegistryResolutionException {
        extensionResolver.clearCache();
    }
}
