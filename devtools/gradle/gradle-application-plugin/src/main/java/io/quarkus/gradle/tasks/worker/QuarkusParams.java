package io.quarkus.gradle.tasks.worker;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

import io.quarkus.bootstrap.model.ApplicationModel;

public interface QuarkusParams extends WorkParameters {
    DirectoryProperty getTargetDirectory();

    MapProperty<String, String> getBuildSystemProperties();

    Property<String> getBaseName();

    Property<ApplicationModel> getAppModel();

    Property<String> getGradleVersion();
}
