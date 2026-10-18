package io.quarkus.apicurio.registry.avro;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class ApicurioRegistryAvroProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.APICURIO_REGISTRY_AVRO);
    }

    @BuildStep
    public void apicurioRegistryAvro(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.avro.AvroKafkaDeserializer",
                "io.apicurio.registry.serde.avro.AvroKafkaSerializer").methods().build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.strategy.SimpleTopicIdStrategy",
                "io.apicurio.registry.serde.strategy.TopicIdStrategy",
                "io.apicurio.registry.serde.avro.DefaultAvroDatumProvider",
                "io.apicurio.registry.serde.avro.ReflectAvroDatumProvider",
                "io.apicurio.registry.serde.avro.ReflectAllowNullAvroDatumProvider",
                "io.apicurio.registry.serde.avro.strategy.RecordIdStrategy",
                "io.apicurio.registry.serde.avro.strategy.QualifiedRecordIdStrategy",
                "io.apicurio.registry.serde.avro.strategy.TopicRecordIdStrategy").methods().fields()
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
        return new ExtensionSslNativeSupportBuildItem(Feature.APICURIO_REGISTRY_AVRO);
    }

}
