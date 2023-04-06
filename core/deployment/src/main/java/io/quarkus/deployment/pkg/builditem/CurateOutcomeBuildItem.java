package io.quarkus.deployment.pkg.builditem;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.builder.item.SimpleBuildItem;

public final class CurateOutcomeBuildItem extends SimpleBuildItem {

    private final ApplicationModel appModel;

    public CurateOutcomeBuildItem(ApplicationModel appModel) {
        this.appModel = appModel;
    }

    public ApplicationModel getApplicationModel() {
        return appModel;
    }
}
