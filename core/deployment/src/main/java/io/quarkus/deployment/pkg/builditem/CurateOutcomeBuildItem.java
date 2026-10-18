package io.quarkus.deployment.pkg.builditem;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents the outcome of the application dependency resolution and
 * model building.
 * This build item exposes the resulting {@link ApplicationModel}
 * to subsequent build steps.
 */
public final class CurateOutcomeBuildItem extends SimpleBuildItem {
    /*
     * The application model resulting from the curation process and Dependency resolution.
     */
    private final ApplicationModel appModel;

    /**
     * Creates a new CurateOutcomeBuildItem.
     *
     * @param appModel the application model
     */
    public CurateOutcomeBuildItem(ApplicationModel appModel) {
        this.appModel = appModel;
    }

    /**
     * Gets the application model.
     *
     * @return the application model
     */
    public ApplicationModel getApplicationModel() {
        return appModel;
    }
}
