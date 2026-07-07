package io.quarkus.gradle.application.internal.codegen.worker;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.internal.execution.worker.QuarkusParams;
import io.quarkus.runtime.LaunchMode;

public interface CodegenWorkerParams extends QuarkusParams {

    ConfigurableFileCollection getSourceDirectories();

    DirectoryProperty getOutputPath();

    Property<LaunchMode> getLaunchMode();
}
