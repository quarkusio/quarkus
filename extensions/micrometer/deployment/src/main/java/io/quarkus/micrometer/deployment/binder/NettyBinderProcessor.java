package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.binder.netty.NettyMetricsProvider;
import io.quarkus.micrometer.runtime.binder.netty.ReactiveNettyMetricsProvider;
import io.quarkus.micrometer.runtime.binder.netty.VertxNettyAllocatorMetricsProvider;
import io.quarkus.micrometer.runtime.binder.netty.VertxNettyEventExecutorMetricsProvider;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * Add support for Netty allocator metrics. Note that
 * various bits of support may not be present at deploy time. Avoid referencing
 * classes that in turn import optional dependencies.
 */
public class NettyBinderProcessor {
    static final String NETTY_ALLOCATOR_METRICS_NAME = "io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics";
    static final Class<?> NETTY_ALLOCATOR_METRICS_CLASS = MicrometerRecorder.getClassForName(NETTY_ALLOCATOR_METRICS_NAME);

    static final String NETTY_EVENT_EXECUTOR_METRICS_NAME = "io.micrometer.core.instrument.binder.netty4.NettyEventExecutorMetrics";
    static final Class<?> NETTY_EVENT_EXECUTOR_METRICS_CLASS = MicrometerRecorder
            .getClassForName(NETTY_EVENT_EXECUTOR_METRICS_NAME);

    static final String NETTY_BYTE_BUF_ALLOCATOR_NAME = "io.netty.buffer.PooledByteBufAllocator";
    static final Class<?> NETTY_BYTE_BUF_ALLOCATOR_CLASS = MicrometerRecorder.getClassForName(NETTY_BYTE_BUF_ALLOCATOR_NAME);

    static final String VERTX_BYTE_BUF_ALLOCATOR_NAME = "io.vertx.core.buffer.impl.VertxByteBufAllocator";
    static final Class<?> VERTX_BYTE_BUF_ALLOCATOR_CLASS = MicrometerRecorder.getClassForName(VERTX_BYTE_BUF_ALLOCATOR_NAME);

    static final String REACTIVE_USAGE_NAME = "org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormUpload";
    static final Class<?> REACTIVE_USAGE_CLASS = MicrometerRecorder.getClassForName(REACTIVE_USAGE_NAME);

    static final String VERTX_NAME = "io.vertx.core.Vertx";
    static final Class<?> VERTX_CLASS = MicrometerRecorder.getClassForName(VERTX_NAME);

    private static abstract class AbstractSupportEnabled implements BooleanSupplier {
        abstract MicrometerConfig getMicrometerConfig();

        Class<?> metricsClass() {
            return NETTY_ALLOCATOR_METRICS_CLASS;
        }

        abstract Class<?> getCheckClass();

        public boolean getAsBoolean() {
            return metricsClass() != null && getCheckClass() != null
                    && getMicrometerConfig().checkBinderEnabledWithDefault(getMicrometerConfig().binder().netty());
        }
    }

    static class NettySupportEnabled extends AbstractSupportEnabled {
        MicrometerConfig mConfig;

        @Override
        MicrometerConfig getMicrometerConfig() {
            return mConfig;
        }

        @Override
        Class<?> getCheckClass() {
            return NETTY_BYTE_BUF_ALLOCATOR_CLASS;
        }
    }

    static class VertxAllocatorSupportEnabled extends AbstractSupportEnabled {
        MicrometerConfig mConfig;

        @Override
        MicrometerConfig getMicrometerConfig() {
            return mConfig;
        }

        @Override
        Class<?> getCheckClass() {
            return VERTX_BYTE_BUF_ALLOCATOR_CLASS;
        }
    }

    static class VertxEventExecutorSupportEnabled extends AbstractSupportEnabled {
        MicrometerConfig mConfig;

        @Override
        MicrometerConfig getMicrometerConfig() {
            return mConfig;
        }

        @Override
        Class<?> metricsClass() {
            return NETTY_EVENT_EXECUTOR_METRICS_CLASS;
        }

        @Override
        Class<?> getCheckClass() {
            return VERTX_CLASS;
        }
    }

    static class ReactiveSupportEnabled extends AbstractSupportEnabled {
        MicrometerConfig mConfig;

        @Override
        MicrometerConfig getMicrometerConfig() {
            return mConfig;
        }

        @Override
        Class<?> getCheckClass() {
            return REACTIVE_USAGE_CLASS;
        }
    }

    @BuildStep(onlyIf = NettySupportEnabled.class)
    void createNettyNettyAllocatorMetrics(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(NettyMetricsProvider.class));
    }

    @BuildStep(onlyIf = VertxAllocatorSupportEnabled.class)
    void createVertxNettyAllocatorMetrics(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(VertxNettyAllocatorMetricsProvider.class));
        // TODO -- VertxByteBufAllocator.DEFAULT ??
    }

    @BuildStep(onlyIf = VertxEventExecutorSupportEnabled.class)
    void createVertxNettyEventExecutorMetrics(BuildProducer<AdditionalBeanBuildItem> beans, Capabilities capabilities) {
        // this is the best we can do, since we cannot check for a Vertx bean, since this itself produces a bean
        if (capabilities.isPresent(Capability.VERTX_CORE)) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(VertxNettyEventExecutorMetricsProvider.class));
        }
    }

    @BuildStep(onlyIf = ReactiveSupportEnabled.class)
    void createReactiveNettyAllocatorMetrics(
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(ReactiveNettyMetricsProvider.class));
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(REACTIVE_USAGE_NAME)
                        .fields()
                        .build());
    }
}
