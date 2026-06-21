package io.quarkus.maven.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.CodeGenerator;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

/**
 * Entry point executed in a forked JVM selected from the Maven toolchain JDK.
 */
public final class MavenAugmentWorker {

    private MavenAugmentWorker() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected a single worker configuration file argument");
        }
        MavenAugmentWorkerConfig config = MavenAugmentWorkerConfig.read(Path.of(args[0]));
        switch (config.mode) {
            case MavenAugmentWorkerConfig.MODE_BUILD:
                runBuild(config);
                break;
            case MavenAugmentWorkerConfig.MODE_CODEGEN:
                runCodegen(config);
                break;
            default:
                throw new IllegalStateException("Unsupported worker mode: " + config.mode);
        }
    }

    private static void runBuild(MavenAugmentWorkerConfig config) throws Exception {
        Properties buildProperties = loadProperties(config.buildPropertiesPath);
        ApplicationModel appModel = ApplicationModelSerializer.deserialize(config.appModelPath);
        try (CuratedApplication curatedApplication = bootstrap(config, appModel, buildProperties)) {
            AugmentAction action = curatedApplication.createAugmentor();
            AugmentResult result = action.createProductionApplication();
            if (config.resultPath != null) {
                MavenAugmentWorkerConfig.writeBuildResult(result, config.resultPath);
            }
        }
    }

    private static void runCodegen(MavenAugmentWorkerConfig config) throws Exception {
        Properties buildProperties = loadProperties(config.buildPropertiesPath);
        ApplicationModel appModel = ApplicationModelSerializer.deserialize(config.appModelPath);
        try (CuratedApplication curatedApplication = bootstrap(config, appModel, buildProperties)) {
            QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(deploymentClassLoader);
            PathCollection sourceParents = PathList.from(config.sourceParents);
            Consumer<Path> noop = path -> {
            };
            CodeGenerator.initAndRun(deploymentClassLoader, sourceParents, config.generatedSourcesDir, config.buildDir,
                    noop, appModel, buildProperties, config.launchMode, config.test);
        }
    }

    private static CuratedApplication bootstrap(MavenAugmentWorkerConfig config, ApplicationModel appModel,
            Properties buildProperties) throws BootstrapException {
        return QuarkusBootstrap.builder()
                .setAppArtifact(appModel.getAppArtifact())
                .setExistingModel(appModel)
                .setIsolateDeployment(true)
                .setBaseClassLoader(MavenAugmentWorker.class.getClassLoader())
                .setBuildSystemProperties(buildProperties)
                .setProjectRoot(config.projectRoot)
                .setBaseName(config.baseName)
                .setOriginalBaseName(config.originalBaseName)
                .setTargetDirectory(config.buildDir)
                .setLocalProjectDiscovery(false)
                .setDependencyInfoProvider(() -> null)
                .build()
                .bootstrap();
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        return properties;
    }
}
