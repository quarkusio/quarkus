package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Deprecated
@Mojo(name = "prepare-tests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class PrepareTestsMojo extends GenerateCodeTestsMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().warn("'prepare-tests' goal is deprecated. Please use 'generate-code-tests' instead");
        super.execute();
    }
}
