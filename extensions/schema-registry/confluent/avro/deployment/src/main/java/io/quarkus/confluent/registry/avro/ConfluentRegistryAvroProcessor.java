package io.quarkus.confluent.registry.avro;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ConfluentRegistryAvroProcessor {

    public static final String CONFLUENT_GROUP_ID = "io.confluent";
    public static final String CONFLUENT_ARTIFACT_ID = "kafka-avro-serializer";

    private static final Logger LOGGER = Logger.getLogger(ConfluentRegistryAvroProcessor.class.getName());
    public static final String CONFLUENT_REPO = "https://packages.confluent.io/maven/";
    public static final String GUIDE_URL = "https://quarkus.io/guides/kafka-schema-registry-avro";

    @BuildStep
    FeatureBuildItem featureAndCheckDependency(CurateOutcomeBuildItem cp) {
        if (findConfluentSerde(cp.getApplicationModel().getDependencies()).isEmpty()) {
            LOGGER.warnf("The application uses the `quarkus-confluent-registry-avro` extension, but does not " +
                    "depend on `%s:%s`. Note that this dependency is only available from the `%s` Maven " +
                    "repository. Check %s for more details.",
                    CONFLUENT_GROUP_ID, CONFLUENT_ARTIFACT_ID, CONFLUENT_REPO, GUIDE_URL);
        }

        return new FeatureBuildItem(Feature.CONFLUENT_REGISTRY_AVRO);
    }

    @BuildStep
    public void confluentRegistryAvro(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        reflectiveClass
                .produce(ReflectiveClassBuildItem.builder("io.confluent.kafka.serializers.KafkaAvroDeserializer",
                        "io.confluent.kafka.serializers.KafkaAvroSerializer").methods().build());
    }

    @BuildStep
    public void configureNative(BuildProducer<NativeImageConfigBuildItem> config, CurateOutcomeBuildItem cp) {
        Optional<ResolvedDependency> serde = findConfluentSerde(cp.getApplicationModel().getDependencies());
        if (serde.isPresent()) {
            String version = serde.get().getVersion();
            NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder();
            if (version.startsWith("7.1") || version.startsWith("7.2")) {
                // Only required for Confluent Serde 7.1.x and 7.2.x
                builder.addRuntimeInitializedClass("io.confluent.kafka.schemaregistry.client.rest.utils.UrlList");
            }
            // Depends on org.apache.avro.reflect.ReflectData$AllowNull since v7.4.5
            builder.addRuntimeInitializedClass("io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils");
            config.produce(builder.build());
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.CONFLUENT_REGISTRY_AVRO);
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
}
