package io.quarkus.agroal.runtime.dev.ui;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DatabaseInspectorRecorder {

    public void setDevConfig(boolean allowSql, String allowedHost) {
        DatabaseInspector.setDevConfig(allowSql, allowedHost);
    }
}
