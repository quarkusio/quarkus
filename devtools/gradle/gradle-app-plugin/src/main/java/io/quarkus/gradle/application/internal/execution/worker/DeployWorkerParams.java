package io.quarkus.gradle.application.internal.execution.worker;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public interface DeployWorkerParams extends QuarkusParams {

    Property<String> getBuildName();

    Property<String> getDeploymentName();

    Property<String> getDeploymentTarget();

    Property<String> getImageSource();

    Property<String> getImageReference();

    RegularFileProperty getDeploymentResultFile();
}
