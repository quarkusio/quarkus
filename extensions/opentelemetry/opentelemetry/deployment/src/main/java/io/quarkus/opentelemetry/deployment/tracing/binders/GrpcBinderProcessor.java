package io.quarkus.opentelemetry.deployment.tracing.binders;

import static io.quarkus.bootstrap.classloading.QuarkusClassLoader.isClassPresentAtRuntime;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.opentelemetry.deployment.tracing.TracerEnabled;
import io.quarkus.opentelemetry.runtime.tracing.binders.grpc.GrpcTracingClientInterceptor;
import io.quarkus.opentelemetry.runtime.tracing.binders.grpc.GrpcTracingServerInterceptor;

public class GrpcBinderProcessor {

    static class GrpcExtensionAvailable implements BooleanSupplier {
        private static final boolean IS_GRPC_EXTENSION_AVAILABLE = isClassPresentAtRuntime(
                "io.quarkus.grpc.runtime.GrpcServerRecorder");

        @Override
        public boolean getAsBoolean() {
            return IS_GRPC_EXTENSION_AVAILABLE;
        }
    }

    @BuildStep(onlyIf = { TracerEnabled.class, GrpcExtensionAvailable.class })
    void grpcTracers(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(GrpcTracingServerInterceptor.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(GrpcTracingClientInterceptor.class));
    }
}
