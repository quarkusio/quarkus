package io.quarkus.grpc.common.deployment;

import java.util.ArrayList;
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
        Collection<ClassInfo> messagesV3 = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.GENERATED_MESSAGE_V3);
        Collection<ClassInfo> messages = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.GENERATED_MESSAGE);
        // We also need to include enums.
        Collection<ClassInfo> enums = combinedIndex.getIndex().getAllKnownImplementations(GrpcDotNames.PROTOCOL_MESSAGE_ENUM);
        Collection<ClassInfo> buildersV3 = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.MESSAGE_BUILDER_V3);
        Collection<ClassInfo> builders = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.MESSAGE_BUILDER);

        Collection<String> reflectedMethodsAndFields = new ArrayList<>(
                messagesV3.size() + messages.size() + enums.size() + buildersV3.size() + builders.size());
        for (ClassInfo message : messagesV3) {
            reflectedMethodsAndFields.add(message.name().toString());
        }
        for (ClassInfo message : messages) {
            reflectedMethodsAndFields.add(message.name().toString());
        }
        for (ClassInfo en : enums) {
            reflectedMethodsAndFields.add(en.name().toString());
        }
        for (ClassInfo builder : buildersV3) {
            reflectedMethodsAndFields.add(builder.name().toString());
        }
        for (ClassInfo builder : builders) {
            reflectedMethodsAndFields.add(builder.name().toString());
        }
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(reflectedMethodsAndFields)
                .methods()
                .fields().build());

        Collection<ClassInfo> lbs = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.LOAD_BALANCER_PROVIDER);
        Collection<ClassInfo> nrs = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.NAME_RESOLVER_PROVIDER);
        Collection<String> reflectedMethodsOnly = new ArrayList<>(lbs.size() + nrs.size());
        for (ClassInfo lb : lbs) {
            reflectedMethodsOnly.add(lb.name().toString());
        }
        for (ClassInfo nr : nrs) {
            reflectedMethodsOnly.add(nr.name().toString());
        }
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(reflectedMethodsOnly).methods().build());

        // Built-In providers:
        reflectiveClass.produce(ReflectiveClassBuildItem
                .builder(DnsNameResolverProvider.class, PickFirstLoadBalancerProvider.class, NettyChannelProvider.class)
                .methods()
                .reason(getClass().getName() + " built-in provider")
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider")
                .reason(getClass().getName() + " built-in provider")
                .methods().build());
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
                .addRuntimeInitializedClass("io.grpc.internal.RetriableStream")
                .addRuntimeInitializedClass("com.google.protobuf.JavaFeaturesProto")
                .addRuntimeInitializedClass("com.google.protobuf.UnsafeUtil");
        return builder.build();
    }

}
