package io.quarkus.gradle.tasks.worker;

import java.nio.file.Path;
import java.util.Properties;

import org.gradle.workers.WorkAction;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;

public abstract class QuarkusWorker<P extends QuarkusParams> implements WorkAction<P> {

    Properties buildSystemProperties() {
        Properties props = new Properties();
        props.putAll(getParameters().getBuildSystemProperties().get());
        return props;
    }

    CuratedApplication createAppCreationContext() throws BootstrapException {
        QuarkusParams params = getParameters();
        Path buildDir = params.getTargetDirectory().getAsFile().get().toPath();
        String baseName = params.getBaseName().get();
        ApplicationModel appModel = params.getAppModel().get();
        return QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(buildDir)
                .setBaseName(baseName)
                .setBuildSystemProperties(buildSystemProperties())
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .setDependencyInfoProvider(() -> null)
                .build().bootstrap();
    }
}
