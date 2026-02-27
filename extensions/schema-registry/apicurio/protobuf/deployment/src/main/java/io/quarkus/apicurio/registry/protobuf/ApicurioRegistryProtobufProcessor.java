package io.quarkus.apicurio.registry.protobuf;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class ApicurioRegistryProtobufProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.APICURIO_REGISTRY_PROTOBUF);
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
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.APICURIO_REGISTRY_PROTOBUF);
    }

}
