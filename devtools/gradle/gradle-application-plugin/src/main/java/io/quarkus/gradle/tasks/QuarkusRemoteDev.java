package io.quarkus.gradle.tasks;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;

public class QuarkusRemoteDev extends QuarkusDev {

    @Inject
    public QuarkusRemoteDev(Configuration quarkusDevDependencies) {
        super("Remote development mode: enables hot deployment on remote JVM with background compilation",
                quarkusDevDependencies);
    }

    protected void modifyDevModeContext(GradleDevModeLauncher.Builder builder) {
        builder.remoteDev(true);
    }
}
