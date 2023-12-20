package io.quarkus.apicurio.registry.protobuf;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
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
    public void apicurioRegistryProtobuf(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.protobug.ProtobufKafkaDeserializer",
                        "io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer").methods().build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.apicurio.registry.serde.strategy.SimpleTopicIdStrategy",
                "io.apicurio.registry.serde.strategy.TopicIdStrategy").methods().fields()
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
        return new ExtensionSslNativeSupportBuildItem(Feature.APICURIO_REGISTRY_PROTOBUF);
    }

}
