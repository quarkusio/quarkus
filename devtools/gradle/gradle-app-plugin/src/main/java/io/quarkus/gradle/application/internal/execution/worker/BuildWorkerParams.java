package io.quarkus.gradle.application.internal.execution.worker;

import org.gradle.api.file.RegularFileProperty;

public interface BuildWorkerParams extends QuarkusParams {
    RegularFileProperty getAugmentResultFile();
}
