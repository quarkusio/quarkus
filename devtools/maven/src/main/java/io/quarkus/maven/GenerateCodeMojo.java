package io.quarkus.maven;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
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
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
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
    boolean skipSourceGeneration = false;

    /**
     * Application launch mode for which to generate the source code.
     */
    @Parameter(defaultValue = "NORMAL", property = "launchMode")
    String mode;

    @Override
    protected boolean beforeExecute() throws MojoExecutionException, MojoFailureException {
        if (mavenProject().getPackaging().equals(ArtifactCoords.TYPE_POM)) {
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
        generateCode(getParentDirs(mavenProject().getCompileSourceRoots()),
                path -> mavenProject().addCompileSourceRoot(path.toString()), false);
    }

    void generateCode(PathCollection sourceParents, Consumer<Path> sourceRegistrar, boolean test)
            throws MojoExecutionException {

        final LaunchMode launchMode;
        if (test) {
            launchMode = LaunchMode.TEST;
        } else if (mavenSession().getGoals().contains("quarkus:dev")) {
            // if the command was 'compile quarkus:dev' then we'll end up with prod launch mode but we want dev
            launchMode = LaunchMode.DEVELOPMENT;
        } else {
            launchMode = LaunchMode.valueOf(mode);
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Bootstrapping Quarkus application in mode " + launchMode);
        }

        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        CuratedApplication curatedApplication = null;
        QuarkusClassLoader deploymentClassLoader = null;
        try {
            curatedApplication = bootstrapApplication(launchMode);
            deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);

            final Class<?> codeGenerator = deploymentClassLoader.loadClass("io.quarkus.deployment.CodeGenerator");
            final Method initAndRun = codeGenerator.getMethod("initAndRun", QuarkusClassLoader.class, PathCollection.class,
                    Path.class, Path.class,
                    Consumer.class, ApplicationModel.class, Properties.class, String.class,
                    boolean.class);
            initAndRun.invoke(null, deploymentClassLoader, sourceParents,
                    generatedSourcesDir(test), buildDir().toPath(),
                    sourceRegistrar, curatedApplication.getApplicationModel(), getBuildSystemProperties(false),
                    launchMode.name(),
                    test);
        } catch (Exception any) {
            throw new MojoExecutionException("Quarkus code generation phase has failed", any);
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
            if (deploymentClassLoader != null) {
                deploymentClassLoader.close();
            }
            // In case of the test mode, we can't share the application model with the test plugins, so we are closing it right away,
            // but we are serializing the application model so the test plugins can deserialize it from disk instead of re-initializing
            // the resolver and re-resolving it as part of the test bootstrap
            if (test && curatedApplication != null) {
                var appModel = curatedApplication.getApplicationModel();
                closeApplication(LaunchMode.TEST);
                if (isSerializeTestModel()) {
                    final int workspaceId = getWorkspaceId();
                    if (workspaceId != 0) {
                        try {
                            BootstrapUtils.writeAppModelWithWorkspaceId(appModel, workspaceId, BootstrapUtils
                                    .getSerializedTestAppModelPath(Path.of(mavenProject().getBuild().getDirectory())));
                        } catch (IOException e) {
                            getLog().warn("Failed to serialize application model", e);
                        }
                    }
                }
            }
        }
    }

    protected boolean isSerializeTestModel() {
        return false;
    }

    protected PathCollection getParentDirs(List<String> sourceDirs) {
        if (sourceDirs.size() == 1) {
            return PathList.of(Path.of(sourceDirs.get(0)).getParent());
        }
        final PathList.Builder builder = PathList.builder();
        for (int i = 0; i < sourceDirs.size(); ++i) {
            final Path parentDir = Path.of(sourceDirs.get(i)).getParent();
            if (!builder.contains(parentDir)) {
                builder.add(parentDir);
            }
        }
        return builder.build();
    }

    private Path generatedSourcesDir(boolean test) {
        return test ? buildDir().toPath().resolve("generated-test-sources") : buildDir().toPath().resolve("generated-sources");
    }
}
