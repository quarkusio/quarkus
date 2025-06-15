package io.quarkus.gradle.tasks.worker;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.CodeGenerator;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;

public abstract class CodeGenWorker extends QuarkusWorker<CodeGenWorkerParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeGenWorker.class);

    public static final String INIT_AND_RUN = "initAndRun";

    @Override
    public void execute() {
        CodeGenWorkerParams params = getParameters();
        Properties props = buildSystemProperties();

        ResolvedDependency appArtifact = params.getAppModel().get().getAppArtifact();
        Path buildDir = params.getTargetDirectory().getAsFile().get().toPath();
        Path generatedSourceDir = params.getOutputPath().get().getAsFile().toPath();

        String gav = appArtifact.getGroupId() + ":" + appArtifact.getArtifactId() + ":" + appArtifact.getVersion();
        LOGGER.info("Generating Quarkus code for {}", gav);
        LOGGER.info("  launch mode:                  {}", params.getLaunchMode().get());
        LOGGER.info("  base name:                    {}", params.getBaseName().get());
        LOGGER.info("  generated source directory:   {}", generatedSourceDir);
        LOGGER.info("  build directory:              {}", buildDir);

        try (CuratedApplication appCreationContext = createAppCreationContext()) {

            QuarkusClassLoader deploymentClassLoader = appCreationContext.createDeploymentClassLoader();
            Class<?> codeGenerator = deploymentClassLoader.loadClass(CodeGenerator.class.getName());

            Method initAndRun;
            try {
                initAndRun = codeGenerator.getMethod(INIT_AND_RUN, QuarkusClassLoader.class, PathCollection.class,
                        Path.class, Path.class, Consumer.class, ApplicationModel.class, Properties.class, String.class,
                        boolean.class);
            } catch (Exception e) {
                throw new GradleException("Quarkus code generation phase has failed", e);
            }

            Consumer<Path> sourceRegistrar = (p) -> {
            };

            LaunchMode launchMode = params.getLaunchMode().get();

            initAndRun.invoke(null,
                    // QuarkusClassLoader classLoader,
                    deploymentClassLoader,
                    // PathCollection sourceParentDirs,
                    PathList.from(params.getSourceDirectories().getFiles().stream().map(File::toPath)
                            .collect(Collectors.toList())),
                    // Path generatedSourcesDir,
                    generatedSourceDir,
                    // Path buildDir,
                    buildDir,
                    // Consumer<Path> sourceRegistrar,
                    sourceRegistrar,
                    // ApplicationModel appModel,
                    appCreationContext.getApplicationModel(),
                    // Properties properties,
                    props,
                    // String launchMode,
                    launchMode.name(),
                    // boolean test
                    launchMode == LaunchMode.TEST);
        } catch (BootstrapException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            // Gradle "abbreviates" the stacktrace to something human-readable, but here the underlying cause might
            // get lost in the error output, so add 'e' to the message.
            throw new GradleException(
                    "Failed to generate sources in the QuarkusGenerateCode task for " + gav + " due to " + e, e);
        }
    }
}
