package io.quarkus.maven;

import java.util.HashMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Builds a container image.
 */
@Mojo(name = "image-build", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ImageBuildMojo extends AbstractImageMojo {

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        systemProperties = new HashMap<>(systemProperties);
        systemProperties.put("quarkus.container-image.build", "true");
        return super.beforeExecute();
    }
}
