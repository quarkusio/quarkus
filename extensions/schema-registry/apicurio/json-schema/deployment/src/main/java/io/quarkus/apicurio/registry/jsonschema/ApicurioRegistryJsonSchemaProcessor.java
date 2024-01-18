package io.quarkus.apicurio.registry.jsonschema;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
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
    public void apicurioRegistryJsonSchema(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaDeserializer",
                        "io.apicurio.registry.serde.jsonschema.JsonSchemaKafkaSerializer").methods().build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.strategy.SimpleTopicIdStrategy",
                "io.apicurio.registry.serde.strategy.TopicIdStrategy",
                "io.apicurio.registry.serde.strategy.QualifiedRecordIdStrategy",
                "io.apicurio.registry.serde.strategy.RecordIdStrategy",
                "io.apicurio.registry.serde.jsonschema.strategy.TopicRecordIdStrategy").methods().fields()
                .build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.DefaultIdHandler",
                "io.apicurio.registry.serde.Legacy4ByteIdHandler",
                "io.apicurio.registry.serde.fallback.DefaultFallbackArtifactProvider",
                "io.apicurio.registry.serde.headers.DefaultHeadersHandler").methods().fields()
                .build());

        String defaultSchemaResolver = "io.apicurio.registry.serde.DefaultSchemaResolver";
        if (QuarkusClassLoader.isClassPresentAtRuntime(defaultSchemaResolver)) {
            // Class not present after 2.2.0.Final
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(defaultSchemaResolver).methods()
                    .fields().build());
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.APICURIO_REGISTRY_JSON_SCHEMA);
    }

}
