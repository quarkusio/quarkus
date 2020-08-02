package io.quarkus.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedRemoteDevModeMain;

/**
 * The dev mojo, that connects to a remote host.
 */
@Mojo(name = "remote-dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RemoteDevMojo extends DevMojo {
    @Override
    protected void modifyDevModeContext(DevModeContext devModeContext) {
        //we use prod mode here as we are going to generate a production
        //application to sync to the server
        devModeContext.setMode(QuarkusBootstrap.Mode.PROD);
        devModeContext.setAlternateEntryPoint(IsolatedRemoteDevModeMain.class.getName());
    }
}
