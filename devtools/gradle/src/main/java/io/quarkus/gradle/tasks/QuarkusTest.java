package io.quarkus.gradle.tasks;

import java.util.function.Consumer;

import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedTestModeMain;

public class QuarkusTest extends QuarkusDev {

    public QuarkusTest() {
        super("Continuous testing mode: enables continuous testing without starting dev mode");
    }

    protected void modifyDevModeContext(GradleDevModeLauncher.Builder builder) {
        builder.entryPointCustomizer(new Consumer<DevModeContext>() {
            @Override
            public void accept(DevModeContext devModeContext) {
                devModeContext.setAlternateEntryPoint(IsolatedTestModeMain.class.getName());
            }
        });
    }
}
