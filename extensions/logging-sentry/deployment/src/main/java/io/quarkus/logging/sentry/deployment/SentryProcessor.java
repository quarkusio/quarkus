package io.quarkus.logging.sentry.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.logging.sentry.SentryConfig;
import io.quarkus.logging.sentry.SentryHandlerValueFactory;
import io.sentry.Breadcrumb;
import io.sentry.SentryBaseEvent;
import io.sentry.SentryEvent;
import io.sentry.SpanContext;
import io.sentry.SpanId;
import io.sentry.SpanStatus;
import io.sentry.protocol.App;
import io.sentry.protocol.Browser;
import io.sentry.protocol.Contexts;
import io.sentry.protocol.DebugImage;
import io.sentry.protocol.DebugMeta;
import io.sentry.protocol.Device;
import io.sentry.protocol.Gpu;
import io.sentry.protocol.Mechanism;
import io.sentry.protocol.Message;
import io.sentry.protocol.OperatingSystem;
import io.sentry.protocol.Request;
import io.sentry.protocol.SdkInfo;
import io.sentry.protocol.SdkVersion;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.SentryPackage;
import io.sentry.protocol.SentryRuntime;
import io.sentry.protocol.SentrySpan;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import io.sentry.protocol.SentryThread;
import io.sentry.protocol.SentryTransaction;
import io.sentry.protocol.User;

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

    @BuildStep
    ReflectiveClassBuildItem addReflection() {
        return new ReflectiveClassBuildItem(true, true,
                Breadcrumb.class.getName(),
                SentryBaseEvent.class.getName(),
                SentryEvent.class.getName(),
                "io.sentry.SentryValues",
                SpanContext.class.getName(),
                SpanStatus.class.getName(),
                SpanId.class.getName(),
                App.class.getName(),
                Browser.class.getName(),
                Contexts.class.getName(),
                DebugImage.class.getName(),
                DebugMeta.class.getName(),
                Device.class.getName(),
                Gpu.class.getName(),
                Mechanism.class.getName(),
                Message.class.getName(),
                OperatingSystem.class.getName(),
                Request.class.getName(),
                SdkInfo.class.getName(),
                SdkVersion.class.getName(),
                SentryException.class.getName(),
                SentryId.class.getName(),
                SentryPackage.class.getName(),
                SentryRuntime.class.getName(),
                SentryStackFrame.class.getName(),
                SentryStackTrace.class.getName(),
                SentryThread.class.getName(),
                SentryTransaction.class.getName(),
                SentrySpan.class.getName(),
                User.class.getName());
    }
}
