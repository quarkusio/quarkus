package io.quarkus.grpc.common.deployment;

import java.util.Collection;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.NettyChannelProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class GrpcCommonProcessor {

    @BuildStep
    public IndexDependencyBuildItem indexProtobuf() {
        // needed with reflection lookup
        return new IndexDependencyBuildItem("com.google.protobuf", "protobuf-java");
    }

    @BuildStep
    public void configureNativeExecutable(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // we force the usage of the reflection invoker.
        Collection<ClassInfo> messagesV3 = combinedIndex.getIndex()
                .getAllKnownSubclasses(GrpcDotNames.GENERATED_MESSAGE_V3);
        for (ClassInfo message : messagesV3) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(message.name().toString()).methods()
                    .fields().build());
        }

        // we force the usage of the reflection invoker.
        Collection<ClassInfo> messages = combinedIndex.getIndex()
                .getAllKnownSubclasses(GrpcDotNames.GENERATED_MESSAGE);
        for (ClassInfo message : messages) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(message.name().toString()).methods()
                    .fields().build());
        }

        // We also need to include enums.
        Collection<ClassInfo> enums = combinedIndex.getIndex()
                .getAllKnownImplementations(GrpcDotNames.PROTOCOL_MESSAGE_ENUM);
        for (ClassInfo en : enums) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(en.name().toString()).methods()
                    .fields().build());
        }

        Collection<ClassInfo> buildersV3 = combinedIndex.getIndex().getAllKnownSubclasses(GrpcDotNames.MESSAGE_BUILDER_V3);
        for (ClassInfo builder : buildersV3) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(builder.name().toString()).methods()
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

        // reflect all of it -- it's a lot
        reflectiveClasses(reflectiveClass, combinedIndex, GrpcDotNames.DESCRIPTOR_PROTOS);

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

    private static void reflectiveClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex,
            DotName className) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                className.toString()).methods().build());

        ClassInfo classByName = combinedIndex.getIndex().getClassByName(className);
        if (classByName == null) {
            return; // should not be here, but let's just make sure no NPE
        }

        Set<DotName> members = classByName.memberClasses();
        for (DotName memberClassName : members) {
            reflectiveClasses(reflectiveClass, combinedIndex, memberClassName);
        }
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
