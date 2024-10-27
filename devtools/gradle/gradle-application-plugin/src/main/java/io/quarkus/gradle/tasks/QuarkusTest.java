package io.quarkus.gradle.tasks;

import java.util.function.Consumer;

import javax.inject.Inject;

import org.gradle.api.artifacts.Configuration;

import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedTestModeMain;
import io.quarkus.gradle.extension.QuarkusPluginExtension;

public abstract class QuarkusTest extends QuarkusDev {

    @Inject
    public QuarkusTest(Configuration quarkusDevConfiguration, QuarkusPluginExtension extension) {
        super(
                "Continuous testing mode: enables continuous testing without starting dev mode",
                quarkusDevConfiguration,
                extension);
    }

    protected void modifyDevModeContext(DevModeCommandLineBuilder builder) {
        builder.entryPointCustomizer(new Consumer<DevModeContext>() {
            @Override
            public void accept(DevModeContext devModeContext) {
                devModeContext.setAlternateEntryPoint(IsolatedTestModeMain.class.getName());
            }
        });
    }
}
