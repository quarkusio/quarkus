package io.quarkus.apicurio.registry.avro;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;

public class ApicurioRegistryAvroProcessor {
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.APICURIO_REGISTRY_AVRO);
    }

    @BuildStep
    public void apicurioRegistryAvro(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
                "io.apicurio.registry.serde.avro.AvroKafkaDeserializer",
                "io.apicurio.registry.serde.avro.AvroKafkaSerializer"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
                "io.apicurio.registry.serde.strategy.SimpleTopicIdStrategy",
                "io.apicurio.registry.serde.strategy.TopicIdStrategy",
                "io.apicurio.registry.serde.avro.DefaultAvroDatumProvider",
                "io.apicurio.registry.serde.avro.ReflectAvroDatumProvider",
                "io.apicurio.registry.serde.avro.strategy.RecordIdStrategy",
                "io.apicurio.registry.serde.avro.strategy.TopicRecordIdStrategy"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
                "io.apicurio.registry.serde.DefaultSchemaResolver",
                "io.apicurio.registry.serde.DefaultIdHandler",
                "io.apicurio.registry.serde.Legacy4ByteIdHandler",
                "io.apicurio.registry.serde.fallback.DefaultFallbackArtifactProvider",
                "io.apicurio.registry.serde.headers.DefaultHeadersHandler"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void apicurioRegistryClient(VertxBuildItem vertx, ApicurioRegistryClient client) {
        client.setup(vertx.getVertx());
    }
}
