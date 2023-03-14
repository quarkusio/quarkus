package io.quarkus.maven;

import java.util.HashMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class DeployMojo extends AbstractDeploymentMojo {

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        systemProperties = new HashMap<>(systemProperties);
        systemProperties.put("quarkus." + getDeployer().name() + ".deploy", "true");
        systemProperties.put("quarkus.container-image.build", String.valueOf(shouldBuildImage()));
        return super.beforeExecute();
    }
}
