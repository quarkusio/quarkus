package io.quarkus.maven;

import java.util.function.Consumer;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedTestModeMain;

/**
 * The test mojo, that starts continuous testing outside of dev mode
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class TestMojo extends DevMojo {
    @Override
    protected void modifyDevModeContext(MavenDevModeLauncher.Builder builder) {
        builder.entryPointCustomizer(new Consumer<DevModeContext>() {
            @Override
            public void accept(DevModeContext devModeContext) {
                devModeContext.setMode(QuarkusBootstrap.Mode.CONTINUOUS_TEST);
                devModeContext.setAlternateEntryPoint(IsolatedTestModeMain.class.getName());
            }
        });
    }
}
