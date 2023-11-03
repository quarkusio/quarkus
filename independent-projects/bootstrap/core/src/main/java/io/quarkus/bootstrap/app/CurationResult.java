package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.model.ApplicationModel;

public class CurationResult {

    private final ApplicationModel appModel;

    public CurationResult(ApplicationModel appModel) {
        this.appModel = appModel;
    }

    public ApplicationModel getApplicationModel() {
        return appModel;
    }
}
