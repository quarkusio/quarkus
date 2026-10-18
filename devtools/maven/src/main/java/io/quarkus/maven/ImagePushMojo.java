package io.quarkus.maven;

import java.util.HashMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Pushes a container image.
 */
@Mojo(name = "image-push", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ImagePushMojo extends AbstractImageMojo {

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        systemProperties = new HashMap<>(systemProperties);
        systemProperties.put("quarkus.container-image.push", "true");
        return super.beforeExecute();
    }
}
