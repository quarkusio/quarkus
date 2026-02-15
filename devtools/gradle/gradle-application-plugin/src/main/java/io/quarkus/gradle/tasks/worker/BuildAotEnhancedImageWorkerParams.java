package io.quarkus.gradle.tasks.worker;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public interface BuildAotEnhancedImageWorkerParams extends QuarkusParams {

    Property<String> getOriginalContainerImage();

    Property<String> getContainerWorkingDirectory();

    RegularFileProperty getAotFile();
}
