package io.quarkus.confluent.registry.protobuf;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ConfluentRegistryProtobufProcessor {

    public static final String CONFLUENT_GROUP_ID = "io.confluent";
    public static final String CONFLUENT_ARTIFACT_ID = "kafka-protobuf-serializer";

    private static final Logger LOGGER = Logger.getLogger(ConfluentRegistryProtobufProcessor.class.getName());
    public static final String CONFLUENT_REPO = "https://packages.confluent.io/maven/";
    public static final String GUIDE_URL = "https://quarkus.io/guides/kafka-schema-registry-avro";

    private static final DotName GENERATED_MESSAGE = DotName.createSimple("com.google.protobuf.GeneratedMessage");
    private static final DotName GENERATED_MESSAGE_V3 = DotName.createSimple("com.google.protobuf.GeneratedMessageV3");
    private static final DotName MESSAGE_BUILDER = DotName.createSimple("com.google.protobuf.GeneratedMessage$Builder");
    private static final DotName MESSAGE_BUILDER_V3 = DotName.createSimple("com.google.protobuf.GeneratedMessageV3$Builder");
    private static final DotName PROTOCOL_MESSAGE_ENUM = DotName.createSimple("com.google.protobuf.ProtocolMessageEnum");
    private static final DotName DESCRIPTOR_PROTOS = DotName.createSimple("com.google.protobuf.DescriptorProtos");

    @BuildStep
    FeatureBuildItem featureAndCheckDependency(CurateOutcomeBuildItem cp) {
        if (findConfluentSerde(cp.getApplicationModel().getDependencies()).isEmpty()) {
            LOGGER.warnf("The application uses the `quarkus-confluent-registry-protobuf` extension, but does not " +
                    "depend on `%s:%s`. Note that this dependency is only available from the `%s` Maven " +
                    "repository. Check %s for more details.",
                    CONFLUENT_GROUP_ID, CONFLUENT_ARTIFACT_ID, CONFLUENT_REPO, GUIDE_URL);
        }

        return new FeatureBuildItem(Feature.CONFLUENT_REGISTRY_PROTOBUF);
    }

    @BuildStep
    public IndexDependencyBuildItem indexProtobuf() {
        return new IndexDependencyBuildItem("com.google.protobuf", "protobuf-java");
    }

    @BuildStep
    public void confluentRegistryProtobuf(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder(
                        "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer",
                        "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer").methods().build());
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
    public void configureNative(BuildProducer<NativeImageConfigBuildItem> config, CurateOutcomeBuildItem cp) {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                .addRuntimeInitializedClass("com.google.protobuf.UnsafeUtil")
                .addRuntimeInitializedClass("com.google.protobuf.JavaFeaturesProto");

        Optional<ResolvedDependency> serde = findConfluentSerde(cp.getApplicationModel().getDependencies());
        if (serde.isPresent()) {
            String version = serde.get().getVersion();
            if (version.startsWith("7.1") || version.startsWith("7.2")) {
                // Only required for Confluent Serde 7.1.x and 7.2.x
                builder.addRuntimeInitializedClass("io.confluent.kafka.schemaregistry.client.rest.utils.UrlList");
            }
        }

        config.produce(builder.build());
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.CONFLUENT_REGISTRY_PROTOBUF);
    }

    private Optional<ResolvedDependency> findConfluentSerde(Collection<ResolvedDependency> dependencies) {
        return dependencies.stream().filter(new Predicate<ResolvedDependency>() {
            @Override
            public boolean test(ResolvedDependency rd) {
                return rd.getGroupId().equalsIgnoreCase(CONFLUENT_GROUP_ID)
                        && rd.getArtifactId().equalsIgnoreCase(CONFLUENT_ARTIFACT_ID);
            }
        }).findAny();
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
