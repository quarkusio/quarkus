package io.quarkus.gradle.tasks;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedRemoteDevModeMain;

public class QuarkusRemoteDev extends QuarkusDev {

    public QuarkusRemoteDev() {
        super("Remote development mode: enables hot deployment on remote JVM with background compilation");
    }

    protected void modifyDevModeContext(DevModeContext devModeContext) {
        devModeContext.setMode(QuarkusBootstrap.Mode.PROD);
        devModeContext.setAlternateEntryPoint(IsolatedRemoteDevModeMain.class.getName());
    }
}
