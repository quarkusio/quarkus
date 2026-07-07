package io.quarkus.gradle.application.internal.execution.worker;

import org.gradle.api.file.RegularFileProperty;

public interface ResolveImageReferencesWorkerParams extends QuarkusParams {

    RegularFileProperty getResolutionReceiptFile();
}
