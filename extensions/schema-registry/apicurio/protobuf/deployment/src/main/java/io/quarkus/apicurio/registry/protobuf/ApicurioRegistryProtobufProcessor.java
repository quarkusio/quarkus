package io.quarkus.apicurio.registry.protobuf;

import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class ApicurioRegistryProtobufProcessor {

    private static final DotName GENERATED_MESSAGE = DotName.createSimple("com.google.protobuf.GeneratedMessage");
    private static final DotName GENERATED_MESSAGE_V3 = DotName.createSimple("com.google.protobuf.GeneratedMessageV3");
    private static final DotName MESSAGE_BUILDER = DotName.createSimple("com.google.protobuf.GeneratedMessage$Builder");
    private static final DotName MESSAGE_BUILDER_V3 = DotName.createSimple("com.google.protobuf.GeneratedMessageV3$Builder");
    private static final DotName PROTOCOL_MESSAGE_ENUM = DotName.createSimple("com.google.protobuf.ProtocolMessageEnum");
    private static final DotName DESCRIPTOR_PROTOS = DotName.createSimple("com.google.protobuf.DescriptorProtos");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.APICURIO_REGISTRY_PROTOBUF);
    }

    @BuildStep
    public IndexDependencyBuildItem indexProtobuf() {
        return new IndexDependencyBuildItem("com.google.protobuf", "protobuf-java");
    }

    @BuildStep
    public void apicurioRegistryProtobuf(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.protobuf.ProtobufKafkaDeserializer",
                        "io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer").methods().build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.strategy.SimpleTopicIdStrategy",
                "io.apicurio.registry.serde.strategy.TopicIdStrategy").methods().fields()
                .build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.Default4ByteIdHandler",
                "io.apicurio.registry.serde.Legacy8ByteIdHandler",
                "io.apicurio.registry.serde.OptimisticFallbackIdHandler",
                "io.apicurio.registry.serde.fallback.DefaultFallbackArtifactProvider",
                "io.apicurio.registry.serde.kafka.headers.DefaultHeadersHandler").methods().fields()
                .build());
    }

    @BuildStep
    public void configureProtobufNative(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // Register all generated protobuf message classes for reflection
        for (ClassInfo message : combinedIndex.getIndex().getAllKnownSubclasses(GENERATED_MESSAGE_V3)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(message.name().toString()).methods()
                    .fields().build());
        }
        for (ClassInfo message : combinedIndex.getIndex().getAllKnownSubclasses(GENERATED_MESSAGE)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(message.name().toString()).methods()
                    .fields().build());
        }

        // Register all protobuf message builder classes
        for (ClassInfo builder : combinedIndex.getIndex().getAllKnownSubclasses(MESSAGE_BUILDER_V3)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(builder.name().toString()).methods()
                    .fields().build());
        }
        for (ClassInfo builder : combinedIndex.getIndex().getAllKnownSubclasses(MESSAGE_BUILDER)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(builder.name().toString()).methods()
                    .fields().build());
        }

        // Register protobuf enum implementations
        for (ClassInfo en : combinedIndex.getIndex().getAllKnownImplementations(PROTOCOL_MESSAGE_ENUM)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(en.name().toString()).methods()
                    .fields().build());
        }

        // Register DescriptorProtos and all its inner classes (needed for parsing file descriptors from the registry)
        registerDescriptorProtos(reflectiveClass, combinedIndex, DESCRIPTOR_PROTOS);
    }

    @BuildStep
    NativeImageConfigBuildItem protobufNativeImageConfig() {
        return NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("com.google.protobuf.UnsafeUtil")
                .addRuntimeInitializedClass("com.google.protobuf.JavaFeaturesProto")
                .build();
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.APICURIO_REGISTRY_PROTOBUF);
    }

    private static void registerDescriptorProtos(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex,
            DotName className) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                className.toString()).methods().fields().build());

        ClassInfo classByName = combinedIndex.getIndex().getClassByName(className);
        if (classByName == null) {
            return;
        }

        Set<DotName> members = classByName.memberClasses();
        for (DotName memberClassName : members) {
            registerDescriptorProtos(reflectiveClass, combinedIndex, memberClassName);
        }
    }
}
