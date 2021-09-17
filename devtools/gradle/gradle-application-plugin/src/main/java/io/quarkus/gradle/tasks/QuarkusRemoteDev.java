package io.quarkus.gradle.tasks;

public class QuarkusRemoteDev extends QuarkusDev {

    public QuarkusRemoteDev() {
        super("Remote development mode: enables hot deployment on remote JVM with background compilation");
    }

    protected void modifyDevModeContext(GradleDevModeLauncher.Builder builder) {
        builder.remoteDev(true);
    }
}
