package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * Add support for gRPC Client and Server metrics. Note that
 * various bits of support may not be present at deploy time. Avoid referencing
 * classes that in turn import optional dependencies.
 */
public class GrpcBinderProcessor {

    static final String CLIENT_GRPC_METRICS_INTERCEPTOR = "io.quarkus.micrometer.runtime.binder.grpc.GrpcMetricsClientInterceptor";
    static final String SERVER_GRPC_METRICS_INTERCEPTOR = "io.quarkus.micrometer.runtime.binder.grpc.GrpcMetricsServerInterceptor";

    static final String CLIENT_INTERCEPTOR = "io.grpc.ClientInterceptor";
    static final String SERVER_INTERCEPTOR = "io.grpc.ServerInterceptor";

    static class GrpcClientSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(CLIENT_INTERCEPTOR)
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder().grpcClient());
        }
    }

    static class GrpcServerSupportEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(SERVER_INTERCEPTOR)
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder().grpcServer());
        }
    }

    @BuildStep(onlyIf = GrpcClientSupportEnabled.class)
    AdditionalBeanBuildItem addGrpcClientMetricInterceptor(BuildProducer<AdditionalIndexedClassesBuildItem> producer) {
        producer.produce(new AdditionalIndexedClassesBuildItem(CLIENT_GRPC_METRICS_INTERCEPTOR));
        return AdditionalBeanBuildItem.unremovableOf(CLIENT_GRPC_METRICS_INTERCEPTOR);
    }

    @BuildStep(onlyIf = GrpcServerSupportEnabled.class)
    AdditionalBeanBuildItem addGrpcServerMetricInterceptor(BuildProducer<AdditionalIndexedClassesBuildItem> producer) {
        producer.produce(new AdditionalIndexedClassesBuildItem(SERVER_GRPC_METRICS_INTERCEPTOR));
        return AdditionalBeanBuildItem.unremovableOf(SERVER_GRPC_METRICS_INTERCEPTOR);
    }

}
