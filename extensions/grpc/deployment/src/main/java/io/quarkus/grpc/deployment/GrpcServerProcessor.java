package io.quarkus.grpc.deployment;

import static io.quarkus.deployment.Feature.GRPC_SERVER;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.internal.ServerImpl;
import io.grpc.netty.NettyChannelProvider;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
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
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.grpc.deployment.devmode.FieldDefinalizingVisitor;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerBuildTimeConfig;
import io.quarkus.grpc.runtime.health.GrpcHealthEndpoint;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class GrpcServerProcessor {

    private static final Logger logger = Logger.getLogger(GrpcServerProcessor.class);

    @BuildStep
    void discoverBindableServices(BuildProducer<BindableServiceBuildItem> bindables,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        Collection<ClassInfo> bindableServices = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(GrpcDotNames.BINDABLE_SERVICE);
        for (ClassInfo service : bindableServices) {
            if (!Modifier.isAbstract(service.flags()) && service.classAnnotation(DotNames.SINGLETON) != null) {
                bindables.produce(new BindableServiceBuildItem(service.name()));
            }
        }
    }

    @BuildStep(onlyIf = IsNormal.class)
    public KubernetesPortBuildItem registerGrpcServiceInKubernetes(List<BindableServiceBuildItem> bindables) {
        if (!bindables.isEmpty()) {
            int port = ConfigProvider.getConfig().getOptionalValue("quarkus.grpc-server.port", Integer.class)
                    .orElse(9000);
            return new KubernetesPortBuildItem(port, GRPC_SERVER);
        }
        return null;
    }

    @BuildStep
    void buildContainerBean(BuildProducer<AdditionalBeanBuildItem> beans,
            List<BindableServiceBuildItem> bindables, BuildProducer<FeatureBuildItem> features) {
        if (!bindables.isEmpty()) {
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
            VertxBuildItem vertx) {
        if (!bindables.isEmpty()) {
            recorder.initializeGrpcServer(vertx.getVertx(), config, shutdown);
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
    public void configureNativeExecutable(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {

        // we force the usage of the reflection invoker.
        Collection<ClassInfo> messages = combinedIndex.getIndex()
                .getAllKnownSubclasses(GrpcDotNames.GENERATED_MESSAGE_V3);
        for (ClassInfo message : messages) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, message.name().toString()));
        }
        Collection<ClassInfo> builders = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.MESSAGE_BUILDER);
        for (ClassInfo builder : builders) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true, builder.name().toString()));
        }

        Collection<ClassInfo> lbs = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.LOAD_BALANCER_PROVIDER);
        for (ClassInfo lb : lbs) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, lb.name().toString()));
        }

        Collection<ClassInfo> nrs = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.NAME_RESOLVER_PROVIDER);
        for (ClassInfo nr : nrs) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, nr.name().toString()));
        }

        // Built-In providers:
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, DnsNameResolverProvider.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, PickFirstLoadBalancerProvider.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
                "io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false, NettyChannelProvider.class));

        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(GRPC_SERVER));
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
    NativeImageConfigBuildItem nativeImageConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.grpc.netty.Utils$ByteBufAllocatorPreferDirectHolder")
                .addRuntimeInitializedClass("io.grpc.netty.Utils$ByteBufAllocatorPreferHeapHolder");
        return builder.build();
    }

}
