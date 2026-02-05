package io.quarkus.modular.spi.items;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.smallrye.common.constraint.Assert;

public final class ApplicationModuleInfoBuildItem extends SimpleBuildItem {
    private final AppModuleModel model;

    public ApplicationModuleInfoBuildItem(final AppModuleModel model) {
        this.model = Assert.checkNotNullParam("model", model);
    }

    public AppModuleModel model() {
        return model;
    }
}
