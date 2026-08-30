package io.quarkus.gradle.application.internal.codegen.worker;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.CodeGenerator;
import io.quarkus.gradle.application.internal.execution.worker.QuarkusWorker;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;

public abstract class CodegenWorker extends QuarkusWorker<CodegenWorkerParams> {

    private static final String INIT_AND_RUN = "initAndRun";

    @Override
    public void execute() {
        CodegenWorkerParams params = getParameters();
        Properties props = buildSystemProperties();

        ResolvedDependency appArtifact = params.getAppModel().get().getAppArtifact();
        Path buildDir = params.getTargetDirectory().getAsFile().get().toPath();
        Path generatedSourceDir = params.getOutputPath().get().getAsFile().toPath();
        String gav = appArtifact.getGroupId() + ":" + appArtifact.getArtifactId() + ":" + appArtifact.getVersion();

        try (CuratedApplication appCreationContext = createAppCreationContext()) {
            QuarkusClassLoader deploymentClassLoader = appCreationContext.createDeploymentClassLoader();
            Class<?> codeGenerator = deploymentClassLoader.loadClass(CodeGenerator.class.getName());

            Method initAndRun;
            try {
                initAndRun = codeGenerator.getMethod(INIT_AND_RUN, QuarkusClassLoader.class, PathCollection.class,
                        Path.class, Path.class,
                        Consumer.class, io.quarkus.bootstrap.model.ApplicationModel.class, Properties.class, String.class,
                        boolean.class);
            } catch (Exception e) {
                throw new GradleException("Quarkus code generation phase has failed", e);
            }

            Consumer<Path> sourceRegistrar = path -> {
            };

            LaunchMode launchMode = params.getLaunchMode().get();

            initAndRun.invoke(null,
                    deploymentClassLoader,
                    PathList.from(
                            params.getSourceDirectories().getFiles().stream().map(File::toPath).collect(Collectors.toList())),
                    generatedSourceDir,
                    buildDir,
                    sourceRegistrar,
                    appCreationContext.getApplicationModel(),
                    props,
                    launchMode.name(),
                    launchMode == LaunchMode.TEST);
        } catch (BootstrapException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new GradleException("Failed to generate sources for " + gav + " due to " + e, e);
        }
    }
}
