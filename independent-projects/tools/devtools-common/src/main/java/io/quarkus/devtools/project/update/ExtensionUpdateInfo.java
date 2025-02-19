package io.quarkus.devtools.project.update;

import static io.quarkus.devtools.project.update.ExtensionUpdateInfo.VersionUpdateType.PLATFORM_MANAGED;
import static io.quarkus.devtools.project.update.ExtensionUpdateInfo.VersionUpdateType.computeVersionUpdateType;

import java.util.Objects;

import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.registry.catalog.Extension;

public final class ExtensionUpdateInfo {
    private final TopExtensionDependency currentDep;
    private final Extension recommendedMetadata;
    private final TopExtensionDependency recommendedDep;
    private final VersionUpdateType versionUpdateType;

    public enum VersionUpdateType {

        /**
         * This extension will be updated as part of the platform update.
         */
        PLATFORM_MANAGED,

        /**
         * This extension version is set, it is recommended to let the version be managed by the platform by dropping the
         * <version>...</version>.
         */
        RECOMMEND_PLATFORM_MANAGED,

        /**
         * This extension is not part of the platform anymore and the <version>...</version> should be added.
         */
        ADD_VERSION,

        /**
         * There is a more recent version of this non platform extension.
         */
        UPDATE_VERSION;

        static VersionUpdateType computeVersionUpdateType(TopExtensionDependency currentDep,
                TopExtensionDependency recommendedDep) {
            if (currentDep.isPlatformExtension() && recommendedDep.isPlatformExtension()) {
                if (currentDep.isNonRecommendedVersion()) {
                    return RECOMMEND_PLATFORM_MANAGED;
                }
                return PLATFORM_MANAGED;
            }
            if (currentDep.isPlatformExtension()) {
                return VersionUpdateType.ADD_VERSION;
            }
            if (recommendedDep.isPlatformExtension()) {
                return VersionUpdateType.RECOMMEND_PLATFORM_MANAGED;
            }
            return VersionUpdateType.UPDATE_VERSION;
        }
    }

    public ExtensionUpdateInfo(TopExtensionDependency currentDep, Extension recommendedMetadata,
            TopExtensionDependency recommendedDep) {
        this.currentDep = currentDep;
        this.recommendedMetadata = recommendedMetadata;
        this.recommendedDep = recommendedDep;
        this.versionUpdateType = computeVersionUpdateType(currentDep, recommendedDep);
    }

    public VersionUpdateType getVersionUpdateType() {
        return versionUpdateType;
    }

    public TopExtensionDependency getCurrentDep() {
        return currentDep;
    }

    public Extension getRecommendedMetadata() {
        return recommendedMetadata;
    }

    public TopExtensionDependency getRecommendedDependency() {
        return recommendedDep;
    }

    public boolean isUpdateRecommended() {
        return !Objects.equals(recommendedDep, currentDep);
    }

    public boolean shouldUpdateExtension() {
        return hasKeyChanged() || !PLATFORM_MANAGED.equals(versionUpdateType);
    }

    public boolean hasKeyChanged() {
        return !currentDep.getKey().equals(recommendedDep.getKey());
    }

    public boolean isSimpleVersionUpdate() {
        return VersionUpdateType.UPDATE_VERSION.equals(getVersionUpdateType())
                || VersionUpdateType.RECOMMEND_PLATFORM_MANAGED.equals(getVersionUpdateType());
    }

    public boolean isVersionUpdate() {
        return !VersionUpdateType.ADD_VERSION.equals(getVersionUpdateType());
    }
}
