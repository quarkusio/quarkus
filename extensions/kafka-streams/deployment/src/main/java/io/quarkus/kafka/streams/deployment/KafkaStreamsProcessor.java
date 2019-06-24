package io.quarkus.kafka.streams.deployment;

import java.io.IOException;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.processor.DefaultPartitionGrouper;
import org.apache.kafka.streams.processor.FailOnInvalidTimestamp;
import org.apache.kafka.streams.processor.internals.StreamsPartitionAssignor;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.rocksdb.util.Environment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRecorder;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;

class KafkaStreamsProcessor {

    private static final String STREAMS_OPTION_PREFIX = "kafka-streams.";

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(RecorderContext recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized,
            BuildProducer<SubstrateResourceBuildItem> nativeLibs) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.KAFKA_STREAMS));

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, StreamsPartitionAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultPartitionGrouper.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultProductionExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, LogAndFailExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, ByteArraySerde.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, FailOnInvalidTimestamp.class));

        // for RocksDB, either add linux64 native lib when targeting containers
        if (isContainerBuild()) {
            nativeLibs.produce(new SubstrateResourceBuildItem("librocksdbjni-linux64.so"));
        }
        // otherwise the native lib of the platform this build runs on
        else {
            nativeLibs.produce(new SubstrateResourceBuildItem(Environment.getJniLibraryFileName("rocksdb")));
        }

        // re-initializing RocksDB to enable load of native libs
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("org.rocksdb.RocksDB"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem processBuildTimeConfig(KafkaStreamsRecorder recorder) {
        Config config = ConfigProvider.getConfig();

        Properties properties = new Properties();
        for (String property : config.getPropertyNames()) {
            if (property.startsWith(STREAMS_OPTION_PREFIX)) {
                properties.setProperty(property.substring(STREAMS_OPTION_PREFIX.length()),
                        config.getValue(property, String.class));
            }
        }

        return new BeanContainerListenerBuildItem(recorder.configure(properties));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureAndLoadRocksDb(KafkaStreamsRecorder recorder, KafkaStreamsRuntimeConfig runtimeConfig) {
        // Explicitly loading RocksDB native libs, as that's normally done from within
        // static initializers which already ran during build
        recorder.loadRocksDb();

        recorder.configureRuntimeProperties(runtimeConfig);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(KafkaStreamsTopologyManager.class);
    }

    private boolean isContainerBuild() {
        String containerRuntime = System.getProperty("native-image.container-runtime");

        if (containerRuntime != null) {
            containerRuntime = containerRuntime.trim().toLowerCase();
            return containerRuntime.equals("docker") || containerRuntime.equals("podman");
        }

        String dockerBuild = System.getProperty("native-image.docker-build");

        if (dockerBuild != null) {
            dockerBuild = dockerBuild.trim().toLowerCase();
            return !"false".equals(dockerBuild);
        }

        return false;
    }
}
