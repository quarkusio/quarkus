package io.quarkus.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.deployment.dev.DevModeCommandLineBuilder;

/**
 * The dev mojo, that connects to a remote host.
 */
@Mojo(name = "remote-dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class RemoteDevMojo extends DevMojo {
    @Override
    protected void modifyDevModeContext(DevModeCommandLineBuilder builder) {
        builder.remoteDev(true);
    }
}
