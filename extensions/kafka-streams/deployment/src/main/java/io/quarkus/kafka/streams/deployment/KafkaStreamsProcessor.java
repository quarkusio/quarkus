package io.quarkus.kafka.streams.deployment;

import java.io.IOException;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.StreamsConfig;
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
import io.quarkus.deployment.builditem.JniBuildItem;
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
            BuildProducer<SubstrateResourceBuildItem> nativeLibs,
            BuildProducer<JniBuildItem> jni) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.KAFKA_STREAMS));

        registerClassesThatAreLoadedThroughReflection(reflectiveClasses);
        addSupportForRocksDbLib(nativeLibs);
        enableLoadOfNativeLibs(reinitialized);
        enableJniForNativeBuild(jni);
    }

    private void registerClassesThatAreLoadedThroughReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        registerCompulsoryClasses(reflectiveClasses);
        registerClassesThatClientMaySpecify(reflectiveClasses);
    }

    private void registerCompulsoryClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, StreamsPartitionAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultPartitionGrouper.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultProductionExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, FailOnInvalidTimestamp.class));
    }

    private void registerClassesThatClientMaySpecify(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        registerExceptionHandler(reflectiveClasses);
        registerDefaultSerdes(reflectiveClasses);
    }

    private void registerExceptionHandler(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        Properties properties = buildKafkaStreamsProperties();
        String exceptionHandlerClassName = properties
                .getProperty(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG);

        if (exceptionHandlerClassName == null) {
            registerDefaultExceptionHandler(reflectiveClasses);
        } else {
            registerClassName(reflectiveClasses, exceptionHandlerClassName);
        }
    }

    private void registerDefaultExceptionHandler(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, LogAndFailExceptionHandler.class));
    }

    private void registerDefaultSerdes(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        Properties properties = buildKafkaStreamsProperties();
        String defaultKeySerdeClass = properties.getProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG);
        String defaultValueSerdeClass = properties.getProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG);

        if (defaultKeySerdeClass != null) {
            registerClassName(reflectiveClasses, defaultKeySerdeClass);
        }
        if (defaultValueSerdeClass != null) {
            registerClassName(reflectiveClasses, defaultValueSerdeClass);
        }
        if (!allDefaultSerdesAreDefinedInProperties(defaultKeySerdeClass, defaultValueSerdeClass)) {
            registerDefaultSerde(reflectiveClasses);
        }
    }

    private void addSupportForRocksDbLib(BuildProducer<SubstrateResourceBuildItem> nativeLibs) {
        // for RocksDB, either add linux64 native lib when targeting containers
        if (isContainerBuild()) {
            nativeLibs.produce(new SubstrateResourceBuildItem("librocksdbjni-linux64.so"));
        }
        // otherwise the native lib of the platform this build runs on
        else {
            nativeLibs.produce(new SubstrateResourceBuildItem(Environment.getJniLibraryFileName("rocksdb")));
        }
    }

    private void enableLoadOfNativeLibs(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("org.rocksdb.RocksDB"));
    }

    private void enableJniForNativeBuild(BuildProducer<JniBuildItem> jni) {
        jni.produce(new JniBuildItem());
    }

    private void registerClassName(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, String defaultKeySerdeClass) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, defaultKeySerdeClass));
    }

    private boolean allDefaultSerdesAreDefinedInProperties(String defaultKeySerdeClass, String defaultValueSerdeClass) {
        return defaultKeySerdeClass != null && defaultValueSerdeClass != null;
    }

    private void registerDefaultSerde(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, ByteArraySerde.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem processBuildTimeConfig(KafkaStreamsRecorder recorder) {
        Properties kafkaStreamsProperties = buildKafkaStreamsProperties();
        return new BeanContainerListenerBuildItem(recorder.configure(kafkaStreamsProperties));
    }

    private Properties buildKafkaStreamsProperties() {
        Config config = ConfigProvider.getConfig();
        Properties kafkaStreamsProperties = new Properties();
        for (String property : config.getPropertyNames()) {
            if (isKafkaStreamsProperty(property)) {
                includeKafkaStreamsProperty(config, kafkaStreamsProperties, property);
            }
        }
        return kafkaStreamsProperties;
    }

    private boolean isKafkaStreamsProperty(String property) {
        return property.startsWith(STREAMS_OPTION_PREFIX);
    }

    private void includeKafkaStreamsProperty(Config config, Properties kafkaStreamsProperties, String property) {
        kafkaStreamsProperties.setProperty(property.substring(STREAMS_OPTION_PREFIX.length()),
                config.getValue(property, String.class));
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
