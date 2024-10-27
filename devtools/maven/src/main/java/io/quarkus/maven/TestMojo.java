package io.quarkus.maven;

import java.util.function.Consumer;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedTestModeMain;
import io.quarkus.runtime.LaunchMode;

/**
 * The test mojo, that starts continuous testing outside of dev mode
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class TestMojo extends DevMojo {

    @Override
    protected LaunchMode getLaunchModeClasspath() {
        return LaunchMode.TEST;
    }

    @Override
    protected void modifyDevModeContext(DevModeCommandLineBuilder builder) {
        builder.entryPointCustomizer(new Consumer<DevModeContext>() {
            @Override
            public void accept(DevModeContext devModeContext) {
                devModeContext.setMode(QuarkusBootstrap.Mode.CONTINUOUS_TEST);
                devModeContext.setAlternateEntryPoint(IsolatedTestModeMain.class.getName());
            }
        });
    }
}
