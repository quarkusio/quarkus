package io.quarkus.confluent.registry.avro;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class ConfluentRegistryAvroProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CONFLUENT_REGISTRY_AVRO);
    }

    @BuildStep
    public void confluentRegistryAvro(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, false,
                        "io.confluent.kafka.serializers.KafkaAvroDeserializer",
                        "io.confluent.kafka.serializers.KafkaAvroSerializer"));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.CONFLUENT_REGISTRY_AVRO);
    }

}
