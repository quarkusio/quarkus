package io.quarkus.deployment.pkg.builditem;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.builder.item.SimpleBuildItem;

public final class CurateOutcomeBuildItem extends SimpleBuildItem {

    private final AppModel effectiveModel;

    public CurateOutcomeBuildItem(AppModel effectiveModel) {
        this.effectiveModel = effectiveModel;
    }

    public AppModel getEffectiveModel() {
        return effectiveModel;
    }
}
