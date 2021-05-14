package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_SERVER;
import static io.quarkus.grpc.deployment.GrpcDotNames.GRPC_SERVICE;
import static java.util.Arrays.asList;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.grpc.BindableService;
import io.grpc.internal.ServerImpl;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.deployment.devmode.FieldDefinalizingVisitor;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerBuildTimeConfig;
import io.quarkus.grpc.runtime.health.GrpcHealthEndpoint;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.supports.context.GrpcEnableRequestContext;
import io.quarkus.grpc.runtime.supports.context.GrpcRequestContextCdiInterceptor;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.netty.deployment.MinNettyAllocatorMaxOrderBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class GrpcServerProcessor {

    private static final Logger logger = Logger.getLogger(GrpcServerProcessor.class);

    private static final String SSL_PREFIX = "quarkus.grpc.server.ssl.";
    private static final String CERTIFICATE = SSL_PREFIX + "certificate";
    private static final String KEY = SSL_PREFIX + "key";
    private static final String KEY_STORE = SSL_PREFIX + "key-store";
    private static final String TRUST_STORE = SSL_PREFIX + "trust-store";

    @BuildStep
    MinNettyAllocatorMaxOrderBuildItem setMinimalNettyMaxOrderSize() {
        return new MinNettyAllocatorMaxOrderBuildItem(3);
    }

    @BuildStep
    void discoverBindableServices(BuildProducer<BindableServiceBuildItem> bindables,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        Collection<ClassInfo> bindableServices = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(GrpcDotNames.BINDABLE_SERVICE);

        for (ClassInfo service : bindableServices) {
            if (Modifier.isAbstract(service.flags())) {
                continue;
            }
            BindableServiceBuildItem item = new BindableServiceBuildItem(service.name());
            for (MethodInfo method : service.methods()) {
                if (method.hasAnnotation(GrpcDotNames.BLOCKING)) {
                    item.registerBlockingMethod(method.name());
                }
            }
            bindables.produce(item);
        }
    }

    @BuildStep
    void validateBindableServices(ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> errors) {
        for (BeanInfo bean : validationPhase.getContext().beans().classBeans().withBeanType(BindableService.class)) {
            //noinspection OptionalGetWithoutIsPresent
            if (bean.getTarget().get().asClass().classAnnotation(GRPC_SERVICE) == null) {
                errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                        new IllegalStateException(
                                "A gRPC service bean must be annotated with io.quarkus.GrpcService: " + bean)));
            }
            if (!bean.getScope().getDotName().equals(BuiltinScope.SINGLETON.getName())) {
                errors.produce(new ValidationPhaseBuildItem.ValidationErrorBuildItem(
                        new IllegalStateException("A gRPC service bean must have the javax.inject.Singleton scope: " + bean)));
            }
        }
    }

    @BuildStep(onlyIf = IsNormal.class)
    KubernetesPortBuildItem registerGrpcServiceInKubernetes(List<BindableServiceBuildItem> bindables) {
        if (!bindables.isEmpty()) {
            int port = ConfigProvider.getConfig().getOptionalValue("quarkus.grpc.server.port", Integer.class)
                    .orElse(9000);
            return new KubernetesPortBuildItem(port, GRPC_SERVER);
        }
        return null;
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans,
            List<BindableServiceBuildItem> bindables, BuildProducer<FeatureBuildItem> features) {
        // @GrpcService is a CDI stereotype
        beans.produce(new AdditionalBeanBuildItem(GrpcService.class));
        beans.produce(new AdditionalBeanBuildItem(GrpcRequestContextCdiInterceptor.class));
        beans.produce(new AdditionalBeanBuildItem(GrpcEnableRequestContext.class));

        if (!bindables.isEmpty() || LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcContainer.class));
            features.produce(new FeatureBuildItem(GRPC_SERVER));
        } else {
            logger.debug("Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
        }
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(GrpcServerRecorder recorder, GrpcConfiguration config,
            ShutdownContextBuildItem shutdown, List<BindableServiceBuildItem> bindables,
            LaunchModeBuildItem launchModeBuildItem,
            VertxBuildItem vertx) {

        // Build the list of blocking methods per service implementation
        Map<String, List<String>> blocking = new HashMap<>();
        for (BindableServiceBuildItem bindable : bindables) {
            if (bindable.hasBlockingMethods()) {
                blocking.put(bindable.serviceClass.toString(), bindable.blockingMethods);
            }
        }

        if (!bindables.isEmpty()) {
            recorder.initializeGrpcServer(vertx.getVertx(), config, shutdown, blocking, launchModeBuildItem.getLaunchMode());
            return new ServiceStartBuildItem(GRPC_SERVER);
        }
        return null;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void definializeGrpcFieldsForDevMode(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        transformers.produce(new BytecodeTransformerBuildItem("io.grpc.internal.InternalHandlerRegistry",
                new FieldDefinalizingVisitor("services", "methods")));
        transformers.produce(new BytecodeTransformerBuildItem(ServerImpl.class.getName(),
                new FieldDefinalizingVisitor("interceptors")));
    }

    @BuildStep
    HealthBuildItem addHealthChecks(GrpcServerBuildTimeConfig config,
            List<BindableServiceBuildItem> bindables,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        if (!bindables.isEmpty()) {
            boolean healthEnabled = config.mpHealthEnabled;

            if (config.grpcHealthEnabled) {
                beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcHealthEndpoint.class));
                healthEnabled = true;
            }

            if (healthEnabled) {
                beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcHealthStorage.class));
            }
            return new HealthBuildItem("io.quarkus.grpc.runtime.health.GrpcHealthCheck",
                    config.mpHealthEnabled);
        } else {
            return null;
        }
    }

    @BuildStep
    void registerSslResources(BuildProducer<NativeImageResourceBuildItem> resourceBuildItem) {
        Config config = ConfigProvider.getConfig();
        for (String sslProperty : asList(CERTIFICATE, KEY, KEY_STORE, TRUST_STORE)) {
            config.getOptionalValue(sslProperty, String.class)
                    .ifPresent(value -> ResourceRegistrationUtils.registerResourceForProperty(resourceBuildItem, value));
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem extensionSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(GRPC_SERVER);
    }
}
