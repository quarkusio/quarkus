package io.quarkus.deployment.mutability;

import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.BUILD_SYSTEM_PROPERTIES;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.DEPLOYMENT_LIB;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.LIB;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.QUARKUS;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PersistentAppModel;
import io.quarkus.deployment.pkg.steps.JarResultBuildStep;

public class ReaugmentTask {

    public static void main(Path appRoot) throws Exception {

        Path deploymentLib = appRoot.resolve(LIB).resolve(DEPLOYMENT_LIB);
        Path buildSystemProps = appRoot.resolve(QUARKUS).resolve(BUILD_SYSTEM_PROPERTIES);
        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(deploymentLib.resolve(JarResultBuildStep.APPMODEL_DAT)))) {
            Properties buildSystemProperties = new Properties();
            try (InputStream buildIn = Files
                    .newInputStream(buildSystemProps)) {
                buildSystemProperties.load(buildIn);
            }

            PersistentAppModel appModel = (PersistentAppModel) in.readObject();
            List<AdditionalDependency> additional = new ArrayList<>();

            if (appModel.getUserProvidersDirectory() != null) {
                System.setProperty("quarkus.package.user-providers-directory", appModel.getUserProvidersDirectory()); //bit of a hack, but keeps things simple
                try (Stream<Path> files = Files.list(appRoot.resolve(appModel.getUserProvidersDirectory()))) {
                    files.forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            if (path.toString().endsWith(".jar")) {
                                additional.add(new AdditionalDependency(path, false, true));
                            }
                        }
                    });
                }
            }

            AppModel existingModel = appModel.getAppModel(appRoot);
            System.setProperty("quarkus.package.type", "mutable-jar");
            try (CuratedApplication bootstrap = QuarkusBootstrap.builder()
                    .setAppArtifact(existingModel.getAppArtifact())
                    .setExistingModel(existingModel)
                    .setRebuild(true)
                    .setBuildSystemProperties(buildSystemProperties)
                    .setBaseName(appModel.getBaseName())
                    .addAdditionalApplicationArchives(additional)
                    .setApplicationRoot(existingModel.getAppArtifact().getPath())
                    .setTargetDirectory(appRoot.getParent())
                    .setBaseClassLoader(ReaugmentTask.class.getClassLoader())
                    .build().bootstrap()) {
                bootstrap.createAugmentor().createProductionApplication();
            }

        }
    }
}
