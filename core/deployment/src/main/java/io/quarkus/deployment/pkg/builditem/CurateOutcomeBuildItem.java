package io.quarkus.deployment.pkg.builditem;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.builder.item.SimpleBuildItem;

public final class CurateOutcomeBuildItem extends SimpleBuildItem {

    private final AppModel effectiveModel;
    private final AppModelResolver resolver;

    public CurateOutcomeBuildItem(AppModel effectiveModel, AppModelResolver resolver) {
        this.effectiveModel = effectiveModel;
        this.resolver = resolver;
    }

    public AppModelResolver getResolver() {
        return resolver;
    }

    public AppModel getEffectiveModel() {
        return effectiveModel;
    }
}
