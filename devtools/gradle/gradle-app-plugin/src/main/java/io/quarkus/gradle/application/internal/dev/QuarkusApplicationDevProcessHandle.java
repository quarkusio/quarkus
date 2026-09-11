package io.quarkus.gradle.application.internal.dev;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

interface QuarkusApplicationDevProcessHandle extends AutoCloseable {

    boolean isAlive();

    CompletionStage<Integer> exitCode();

    default Optional<String> devUiUrl() {
        return Optional.empty();
    }

    @Override
    void close() throws Exception;
}
