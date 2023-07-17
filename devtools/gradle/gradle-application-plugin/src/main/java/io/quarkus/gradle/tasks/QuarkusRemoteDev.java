package io.quarkus.gradle.tasks;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

public abstract class QuarkusRemoteDev extends QuarkusDev {

    @Inject
    public QuarkusRemoteDev(Configuration quarkusDevConfiguration, QuarkusPluginExtension extension) {
        super(
                "Remote development mode: enables hot deployment on remote JVM with background compilation",
                quarkusDevConfiguration,
                extension);
    }

    protected void modifyDevModeContext(GradleDevModeLauncher.Builder builder) {
        builder.remoteDev(true);
    }
}
