package io.quarkus.gradle.tasks.worker;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

import io.quarkus.runtime.LaunchMode;

public interface CodeGenWorkerParams extends QuarkusParams {

    ConfigurableFileCollection getSourceDirectories();

    DirectoryProperty getOutputPath();

    Property<LaunchMode> getLaunchMode();
}
