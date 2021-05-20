package io.quarkus.maven;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PathsCollection;

@Mojo(name = "generate-code", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateCodeMojo extends QuarkusBootstrapMojo {

    /**
     * Skip the execution of this mojo
     */
    @Parameter(defaultValue = "false", property = "quarkus.generate-code.skip", alias = "quarkus.prepare.skip")
    private boolean skipSourceGeneration = false;

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

        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        try {

            final CuratedApplication curatedApplication = bootstrapApplication();

            QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            final Class<?> codeGenerator = deploymentClassLoader.loadClass("io.quarkus.deployment.CodeGenerator");
            final Method initAndRun = codeGenerator.getMethod("initAndRun", ClassLoader.class, PathsCollection.class,
                    Path.class,
                    Path.class,
                    Consumer.class, AppModel.class, Map.class);
            initAndRun.invoke(null, deploymentClassLoader,
                    PathsCollection.of(sourcesDir),
                    generatedSourcesDir(test),
                    buildDir().toPath(),
                    sourceRegistrar,
                    curatedApplication.getAppModel(),
                    mavenProject().getProperties());
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
