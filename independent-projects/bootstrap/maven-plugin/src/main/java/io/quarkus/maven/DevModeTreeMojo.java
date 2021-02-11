/**
 *
 */
package io.quarkus.maven;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Displays Quarkus application dependency tree used to set up the classpath for the dev mode.
 */
@Mojo(name = "dev-mode-tree", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevModeTreeMojo extends AbstractTreeMojo {
    @Override
    protected void setupResolver(BootstrapAppModelResolver modelResolver) {
        modelResolver.setDevMode(true);
    }
}
