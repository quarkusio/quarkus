package io.quarkus.gradle.application.internal.dev;

import java.io.IOException;
import java.util.function.Consumer;

import io.quarkus.deployment.dev.BuildOutputChangesServer;
import io.quarkus.deployment.dev.BuildOutputLiveReloadState;

@FunctionalInterface
interface BuildOutputChangesServerFactory {

    BuildOutputChangesServer create(Consumer<BuildOutputLiveReloadState> stateListener) throws IOException;
}
