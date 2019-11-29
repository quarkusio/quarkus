package io.quarkus.logging.sentry.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.logging.sentry.SentryConfig;
import io.quarkus.logging.sentry.SentryHandlerValueFactory;

class SentryProcessor {

    private static final String FEATURE = "sentry";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem addSentryLogHandler(final SentryConfig sentryConfig,
            final SentryHandlerValueFactory sentryHandlerValueFactory) {
        return new LogHandlerBuildItem(sentryHandlerValueFactory.create(sentryConfig));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

}
