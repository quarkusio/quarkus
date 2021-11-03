package io.quarkus.deployment.pkg.builditem;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.builder.item.SimpleBuildItem;

public final class CurateOutcomeBuildItem extends SimpleBuildItem {

    private final ApplicationModel appModel;

    private AppModel effectiveModel;

    public CurateOutcomeBuildItem(ApplicationModel appModel) {
        this.appModel = appModel;
    }

    /**
     * @deprecated in favor of {@link #getApplicationModel()}
     * @return application model
     */
    @Deprecated
    public AppModel getEffectiveModel() {
        return effectiveModel == null ? effectiveModel = BootstrapUtils.convert(appModel) : effectiveModel;
    }

    public ApplicationModel getApplicationModel() {
        return appModel;
    }
}
