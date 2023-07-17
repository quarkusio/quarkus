package io.quarkus.grpc.common.deployment;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;

import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.NettyChannelProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class GrpcCommonProcessor {

    @BuildStep
    public void configureNativeExecutable(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // we force the usage of the reflection invoker.
        Collection<ClassInfo> messages = combinedIndex.getIndex()
                .getAllKnownSubclasses(GrpcDotNames.GENERATED_MESSAGE_V3);
        for (ClassInfo message : messages) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(message.name().toString()).methods()
                    .fields().build());
        }

        // We also need to include enums.
        Collection<ClassInfo> enums = combinedIndex.getIndex()
                .getAllKnownSubclasses(GrpcDotNames.PROTOCOL_MESSAGE_ENUM);
        for (ClassInfo en : enums) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(en.name().toString()).methods()
                    .fields().build());
        }

        Collection<ClassInfo> builders = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.MESSAGE_BUILDER);
        for (ClassInfo builder : builders) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(builder.name().toString()).methods()
                    .fields().build());
        }

        Collection<ClassInfo> lbs = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.LOAD_BALANCER_PROVIDER);
        for (ClassInfo lb : lbs) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(lb.name().toString()).methods()
                    .build());
        }

        Collection<ClassInfo> nrs = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.NAME_RESOLVER_PROVIDER);
        for (ClassInfo nr : nrs) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(nr.name().toString()).methods()
                    .build());
        }

        // Built-In providers:
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(DnsNameResolverProvider.class).methods()
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(PickFirstLoadBalancerProvider.class)
                .methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider")
                .methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(NettyChannelProvider.class).methods()
                .build());
    }

    @BuildStep
    NativeImageConfigBuildItem nativeImageConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("io.grpc.netty.Utils$ByteBufAllocatorPreferDirectHolder")
                .addRuntimeInitializedClass("io.grpc.netty.Utils$ByteBufAllocatorPreferHeapHolder")
                // substitutions are runtime-only, Utils have to be substituted until we cannot use EPoll
                // in native. NettyServerBuilder and NettyChannelBuilder would "bring in" Utils in build time
                // if they were not marked as runtime initialized:
                .addRuntimeInitializedClass("io.grpc.netty.Utils")
                .addRuntimeInitializedClass("io.grpc.netty.NettyServerBuilder")
                .addRuntimeInitializedClass("io.grpc.netty.NettyChannelBuilder")
                .addRuntimeInitializedClass("io.grpc.internal.RetriableStream");
        return builder.build();
    }

}
