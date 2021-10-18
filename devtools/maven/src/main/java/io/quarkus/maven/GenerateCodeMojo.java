package io.quarkus.maven;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.runtime.LaunchMode;

// in the PROCESS_RESOURCES phase because we want the config to be available
// by the time code gen providers are triggered (the resources plugin copies the config files
// to the destination location at the beginning of this phase)
@Mojo(name = "generate-code", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateCodeMojo extends QuarkusBootstrapMojo {

    /**
     * Skip the execution of this mojo
     */
    @Parameter(defaultValue = "false", property = "quarkus.generate-code.skip", alias = "quarkus.prepare.skip")
    private boolean skipSourceGeneration = false;

    @Parameter(defaultValue = "NORMAL", property = "launchMode")
    String mode;

    @Override
    protected boolean beforeExecute() throws MojoExecutionException, MojoFailureException {
        if (mavenProject().getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping build goal");
            return false;
        }
        if (skipSourceGeneration) {
            getLog().info("Skipping Quarkus code generation");
            return false;
        }
        return true;
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        String projectDir = mavenProject().getBasedir().getAbsolutePath();
        Path sourcesDir = Paths.get(projectDir, "src", "main");
        generateCode(sourcesDir, path -> mavenProject().addCompileSourceRoot(path.toString()), false);
    }

    void generateCode(Path sourcesDir,
            Consumer<Path> sourceRegistrar,
            boolean test) throws MojoFailureException, MojoExecutionException {

        final LaunchMode launchMode = test ? LaunchMode.TEST : LaunchMode.valueOf(mode);
        if (getLog().isDebugEnabled()) {
            getLog().debug("Bootstrapping Quarkus application in mode " + launchMode);
        }

        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        try {

            final CuratedApplication curatedApplication = bootstrapApplication(launchMode);

            QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            final Class<?> codeGenerator = deploymentClassLoader.loadClass("io.quarkus.deployment.CodeGenerator");
            final Method initAndRun = codeGenerator.getMethod("initAndRun", ClassLoader.class, PathsCollection.class,
                    Path.class,
                    Path.class,
                    Consumer.class, ApplicationModel.class, Properties.class, String.class);
            initAndRun.invoke(null, deploymentClassLoader,
                    PathsCollection.of(sourcesDir),
                    generatedSourcesDir(test),
                    buildDir().toPath(),
                    sourceRegistrar,
                    curatedApplication.getApplicationModel(),
                    mavenProject().getProperties(), launchMode.name());
        } catch (Exception any) {
            throw new MojoExecutionException("Quarkus code generation phase has failed", any);
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }

    private Path generatedSourcesDir(boolean test) {
        return test ? buildDir().toPath().resolve("generated-test-sources") : buildDir().toPath().resolve("generated-sources");
    }
}
