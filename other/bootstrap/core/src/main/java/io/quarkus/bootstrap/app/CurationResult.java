package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.util.BootstrapUtils;

public class CurationResult {

    private final ApplicationModel appModel;

    public CurationResult(ApplicationModel appModel) {
        this.appModel = appModel;
    }

    /**
     * @deprecated in favor of {@link #getApplicationModel()}
     * @return AppModel
     */
    @Deprecated
    public AppModel getAppModel() {
        return BootstrapUtils.convert(appModel);
    }

    public ApplicationModel getApplicationModel() {
        return appModel;
    }
}
