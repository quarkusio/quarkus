package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-code-tests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateCodeTestsMojo extends GenerateCodeMojo {
    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        generateCode(getParentDirs(mavenProject().getTestCompileSourceRoots()),
                path -> mavenProject().addTestCompileSourceRoot(path.toString()), true);
    }
}
