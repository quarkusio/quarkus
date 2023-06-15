package io.quarkus.devtools.project.update;

import io.quarkus.devtools.project.state.TopExtensionDependency;
import io.quarkus.registry.catalog.Extension;

public final class ExtensionUpdateInfo {
    private final TopExtensionDependency currentDep;
    private final Extension recommendedMetadata;
    private final TopExtensionDependency recommendedDep;

    public ExtensionUpdateInfo(TopExtensionDependency currentDep, Extension recommendedMetadata,
            TopExtensionDependency recommendedDep) {
        this.currentDep = currentDep;
        this.recommendedMetadata = recommendedMetadata;
        this.recommendedDep = recommendedDep;
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
        return recommendedDep != currentDep;
    }
}
