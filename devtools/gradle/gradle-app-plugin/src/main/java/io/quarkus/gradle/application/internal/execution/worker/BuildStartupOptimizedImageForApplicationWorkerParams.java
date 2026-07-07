package io.quarkus.gradle.application.internal.execution.worker;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

public interface BuildStartupOptimizedImageForApplicationWorkerParams extends QuarkusParams {

    Property<String> getOriginalContainerImage();

    Property<String> getContainerWorkingDirectory();

    Property<QuarkusApplicationJvmStartupArchiveType> getArchiveType();

    RegularFileProperty getArchiveFile();

    DirectoryProperty getArchiveDirectory();

    RegularFileProperty getStartupOptimizedImageResultFile();
}
