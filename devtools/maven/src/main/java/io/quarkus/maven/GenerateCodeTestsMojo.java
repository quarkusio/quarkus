package io.quarkus.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-code-tests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateCodeTestsMojo extends GenerateCodeMojo {
    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        String projectDir = mavenProject().getBasedir().getAbsolutePath();
        Path testSources = Paths.get(projectDir, "src", "test");
        generateCode(testSources, path -> mavenProject().addTestCompileSourceRoot(path.toString()), true);
    }
}