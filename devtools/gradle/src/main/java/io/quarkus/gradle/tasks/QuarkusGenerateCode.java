package io.quarkus.gradle.tasks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.deployment.CodeGenerator;

public class QuarkusGenerateCode extends QuarkusTask {

    public static final String INIT_AND_RUN = "initAndRun";
    private Set<Path> sourcesDirectories;
    private Consumer<Path> sourceRegistrar;
    private boolean test = false;

    public QuarkusGenerateCode() {
        super("Performs Quarkus pre-build preparations, such as sources generation");
    }

    @TaskAction
    public void prepareQuarkus() {
        getLogger().lifecycle("preparing quarkus application");

        final AppArtifact appArtifact = extension().getAppArtifact();
        appArtifact.setPaths(QuarkusGradleUtils.getOutputPaths(getProject()));

        final AppModelResolver modelResolver = extension().getAppModelResolver();
        final Properties realProperties = getBuildSystemProperties(appArtifact);

        Path buildDir = getProject().getBuildDir().toPath();
        try (CuratedApplication appCreationContext = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setAppModelResolver(modelResolver)
                .setTargetDirectory(buildDir)
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(realProperties)
                .setAppArtifact(appArtifact)
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {

            final Convention convention = getProject().getConvention();
            JavaPluginConvention javaConvention = convention.findPlugin(JavaPluginConvention.class);
            if (javaConvention != null) {
                String generateSourcesDir = test ? "quarkus-test-generated-sources" : "quarkus-generated-sources";
                final SourceSet generatedSources = javaConvention.getSourceSets().create(generateSourcesDir);
                generatedSources.getOutput().dir(generateSourcesDir);
                List<Path> paths = new ArrayList<>();
                generatedSources.getOutput()
                        .filter(f -> f.getName().equals(generateSourcesDir))
                        .forEach(f -> paths.add(f.toPath()));
                if (paths.isEmpty()) {
                    throw new GradleException("Failed to create quarkus-generated-sources");
                }

                getLogger().debug("Will trigger preparing sources for source directory: {} buildDir: {}",
                        sourcesDirectories, getProject().getBuildDir().getAbsolutePath());

                QuarkusClassLoader deploymentClassLoader = appCreationContext.createDeploymentClassLoader();
                Class<?> codeGenerator = deploymentClassLoader.loadClass(CodeGenerator.class.getName());

                Optional<Method> initAndRun = Arrays.stream(codeGenerator.getMethods())
                        .filter(m -> m.getName().equals(INIT_AND_RUN))
                        .findAny();
                if (!initAndRun.isPresent()) {
                    throw new GradleException("Failed to find " + INIT_AND_RUN + " method in " + CodeGenerator.class.getName());
                }
                initAndRun.get().invoke(null, deploymentClassLoader,
                        sourcesDirectories,
                        paths.iterator().next(),
                        buildDir,
                        sourceRegistrar,
                        appCreationContext.getAppModel());

            }
        } catch (BootstrapException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new GradleException("Failed to generate sources in the QuarkusPrepare task", e);
        }
    }

    public void setSourcesDirectories(Set<Path> sourcesDirectories) {
        this.sourcesDirectories = sourcesDirectories;
    }

    public void setSourceRegistrar(Consumer<Path> sourceRegistrar) {
        this.sourceRegistrar = sourceRegistrar;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
