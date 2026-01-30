package io.quarkus.apicurio.registry.jsonschema;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class ApicurioRegistryJsonSchemaProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.APICURIO_REGISTRY_JSON_SCHEMA);
    }

    @BuildStep
    public void apicurioRegistryJsonSchema(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer",
                        "io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer").methods().build());

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
        return new ExtensionSslNativeSupportBuildItem(Feature.APICURIO_REGISTRY_JSON_SCHEMA);
    }

}
