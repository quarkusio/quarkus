package io.quarkus.gradle.tasks;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Internal;

import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.deployment.dev.IsolatedTestModeMain;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.runtime.LaunchMode;

public abstract class QuarkusTest extends QuarkusDev {

    @Inject
    public QuarkusTest(Configuration quarkusDevConfiguration, QuarkusPluginExtension extension) {
        super(
                "Continuous testing mode: enables continuous testing without starting dev mode",
                quarkusDevConfiguration,
                extension);
    }

    @Override
    protected void modifyDevModeContext(DevModeCommandLineBuilder builder) {
        builder.entryPointCustomizer(
                devModeContext -> devModeContext.setAlternateEntryPoint(IsolatedTestModeMain.class.getName()));
    }

    @Override
    @Internal
    protected LaunchMode getLaunchMode() {
        return LaunchMode.TEST;
    }
}
