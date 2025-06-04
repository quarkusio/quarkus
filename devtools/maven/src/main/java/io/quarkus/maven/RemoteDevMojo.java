package io.quarkus.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.runtime.LaunchMode;

/**
 * The dev mojo, that connects to a remote host.
 */
@Mojo(name = "remote-dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class RemoteDevMojo extends DevMojo {
    @Override
    protected void modifyDevModeContext(DevModeCommandLineBuilder builder) {
        builder.remoteDev(true);
    }

    @Override
    protected LaunchMode getLaunchModeClasspath() {
        // For remote-dev we should match the dependency model on the service side, which is a production mutable-jar,
        // so we return LaunchMode.NORMAL, but we need to enable workspace discovery to be able to watch for source code changes
        project.getProperties().putIfAbsent(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY, "true");
        return LaunchMode.NORMAL;
    }
}
