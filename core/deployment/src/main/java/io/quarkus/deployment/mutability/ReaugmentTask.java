package io.quarkus.deployment.mutability;

import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.*;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PersistentAppModel;
import io.quarkus.deployment.pkg.steps.JarResultBuildStep;

public class ReaugmentTask {

    public static void main(Path appRoot) throws Exception {

        Path deploymentLib = appRoot.resolve(LIB).resolve(DEPLOYMENT_LIB);
        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(deploymentLib.resolve(JarResultBuildStep.APPMODEL_DAT)))) {
            Properties buildSystemProperties = new Properties();
            try (InputStream buildIn = Files
                    .newInputStream(deploymentLib.resolve(BUILD_SYSTEM_PROPERTIES))) {
                buildSystemProperties.load(buildIn);
            }

            PersistentAppModel appModel = (PersistentAppModel) in.readObject();

            AppModel existingModel = appModel.getAppModel(appRoot);
            System.setProperty("quarkus.package.type", "fast-jar");
            try (CuratedApplication bootstrap = QuarkusBootstrap.builder()
                    .setAppArtifact(existingModel.getAppArtifact())
                    .setExistingModel(existingModel)
                    .setRebuild(true)
                    .setBuildSystemProperties(buildSystemProperties)
                    .setBaseName(appModel.getBaseName())
                    .setApplicationRoot(existingModel.getAppArtifact().getPath())
                    .setTargetDirectory(appRoot.getParent())
                    .setBaseClassLoader(ReaugmentTask.class.getClassLoader())
                    .build().bootstrap()) {
                bootstrap.createAugmentor().createProductionApplication();
            }

        }
    }
}
