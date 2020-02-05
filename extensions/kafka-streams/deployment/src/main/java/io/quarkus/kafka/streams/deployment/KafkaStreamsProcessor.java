package io.quarkus.kafka.streams.deployment;

import java.io.IOException;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.kafka.streams.runtime.HotReplacementInterceptor;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRecorder;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsTopologyManager;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class KafkaStreamsProcessor {

    private static final String STREAMS_OPTION_PREFIX = "kafka-streams.";

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            LaunchModeBuildItem launchMode,
            NativeConfig config) throws IOException {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.KAFKA_STREAMS));

        registerClassesThatAreLoadedThroughReflection(reflectiveClasses, launchMode);
        addSupportForRocksDbLib(nativeLibs, config);
        enableLoadOfNativeLibs(reinitialized);
    }

    private void registerClassesThatAreLoadedThroughReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode) {
        registerCompulsoryClasses(reflectiveClasses);
        registerClassesThatClientMaySpecify(reflectiveClasses, launchMode);
    }

    private void registerCompulsoryClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, StreamsPartitionAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultPartitionGrouper.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, DefaultProductionExceptionHandler.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, false, FailOnInvalidTimestamp.class));
    }

    private void registerClassesThatClientMaySpecify(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode) {
        Properties properties = buildKafkaStreamsProperties(launchMode.getLaunchMode());
        registerExceptionHandler(reflectiveClasses, properties);
        registerDefaultSerdes(reflectiveClasses, properties);
    }

    private void registerExceptionHandler(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Properties kafkaStreamsProperties) {
        String exceptionHandlerClassName = kafkaStreamsProperties
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

    private void registerDefaultSerdes(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Properties kafkaStreamsProperties) {
        String defaultKeySerdeClass = kafkaStreamsProperties.getProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG);
        String defaultValueSerdeClass = kafkaStreamsProperties.getProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG);

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

    private void addSupportForRocksDbLib(BuildProducer<NativeImageResourceBuildItem> nativeLibs, NativeConfig nativeConfig) {
        // for RocksDB, either add linux64 native lib when targeting containers
        if (nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild) {
            nativeLibs.produce(new NativeImageResourceBuildItem("librocksdbjni-linux64.so"));
        }
        // otherwise the native lib of the platform this build runs on
        else {
            nativeLibs.produce(new NativeImageResourceBuildItem(Environment.getJniLibraryFileName("rocksdb")));
        }
    }

    private void enableLoadOfNativeLibs(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("org.rocksdb.RocksDB"));
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
    BeanContainerListenerBuildItem processBuildTimeConfig(KafkaStreamsRecorder recorder, LaunchModeBuildItem launchMode) {
        Properties kafkaStreamsProperties = buildKafkaStreamsProperties(launchMode.getLaunchMode());
        return new BeanContainerListenerBuildItem(recorder.configure(kafkaStreamsProperties));
    }

    private Properties buildKafkaStreamsProperties(LaunchMode launchMode) {
        Config config = ConfigProvider.getConfig();
        Properties kafkaStreamsProperties = new Properties();
        for (String property : config.getPropertyNames()) {
            if (isKafkaStreamsProperty(property)) {
                includeKafkaStreamsProperty(config, kafkaStreamsProperties, property);
            }
        }

        if (launchMode == LaunchMode.DEVELOPMENT) {
            addHotReplacementInterceptor(kafkaStreamsProperties);
        }

        return kafkaStreamsProperties;
    }

    private void addHotReplacementInterceptor(Properties kafkaStreamsProperties) {
        String interceptorConfig = HotReplacementInterceptor.class.getName();
        Object originalInterceptorConfig = kafkaStreamsProperties
                .get(StreamsConfig.consumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG));

        if (originalInterceptorConfig != null) {
            interceptorConfig = interceptorConfig + "," + originalInterceptorConfig;
        }

        kafkaStreamsProperties.put(StreamsConfig.consumerPrefix(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG), interceptorConfig);
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

    @BuildStep
    void addHealthChecks(KafkaStreamsBuildTimeConfig buildTimeConfig, BuildProducer<HealthBuildItem> healthChecks) {
        healthChecks.produce(
                new HealthBuildItem(
                        "io.quarkus.kafka.streams.runtime.health.KafkaStreamsTopicsHealthCheck",
                        buildTimeConfig.healthEnabled, "kafka-streams"));
        healthChecks.produce(
                new HealthBuildItem(
                        "io.quarkus.kafka.streams.runtime.health.KafkaStreamsStateHealthCheck",
                        buildTimeConfig.healthEnabled, "kafka-streams"));
    }
}
