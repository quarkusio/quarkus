package io.quarkus.gradle.application.internal.dev;

import io.quarkus.deployment.dev.DevModeContext;

@FunctionalInterface
interface QuarkusApplicationDevProcessLauncher {

    QuarkusApplicationDevProcessHandle launch(DevModeContext.ExternalBuildOutputTransport transport) throws Exception;
}
