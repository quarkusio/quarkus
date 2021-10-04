package io.quarkus.kafka.streams.deployment;

import static io.quarkus.kafka.streams.runtime.KafkaStreamsPropertiesUtil.buildKafkaStreamsProperties;

import java.io.IOException;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.processor.DefaultPartitionGrouper;
import org.apache.kafka.streams.processor.FailOnInvalidTimestamp;
import org.apache.kafka.streams.processor.internals.StreamsPartitionAssignor;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;
import org.rocksdb.util.Environment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsProducer;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRecorder;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsSupport;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class KafkaStreamsProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            LaunchModeBuildItem launchMode,
            NativeConfig config) throws IOException {

        feature.produce(new FeatureBuildItem(Feature.KAFKA_STREAMS));

        registerClassesThatAreLoadedThroughReflection(reflectiveClasses, launchMode);
        registerClassesThatAreAccessedViaJni(jniRuntimeAccessibleClasses);
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
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, true,
                org.apache.kafka.streams.processor.internals.assignment.HighAvailabilityTaskAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, true,
                org.apache.kafka.streams.processor.internals.assignment.StickyTaskAssignor.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, true,
                org.apache.kafka.streams.processor.internals.assignment.FallbackPriorTaskAssignor.class));
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

    private void registerClassesThatAreAccessedViaJni(BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses) {
        jniRuntimeAccessibleClasses
                .produce(new JniRuntimeAccessBuildItem(true, false, false, RocksDBException.class, Status.class));
    }

    private void addSupportForRocksDbLib(BuildProducer<NativeImageResourceBuildItem> nativeLibs, NativeConfig nativeConfig) {
        // for RocksDB, either add linux64 native lib when targeting containers
        if (nativeConfig.isContainerBuild()) {
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
    void processBuildTimeConfig(KafkaStreamsRecorder recorder, LaunchModeBuildItem launchMode,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        Properties kafkaStreamsProperties = buildKafkaStreamsProperties(launchMode.getLaunchMode());

        // create KafkaStreamsSupport as a synthetic bean
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(KafkaStreamsSupport.class)
                .scope(Singleton.class)
                .supplier(recorder.kafkaStreamsSupportSupplier(kafkaStreamsProperties))
                .done());

        // make the producer an unremoveable bean
        additionalBeans
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(KafkaStreamsProducer.class).setUnremovable().build());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadRocksDb(KafkaStreamsRecorder recorder, KafkaStreamsRuntimeConfig runtimeConfig) {
        // Explicitly loading RocksDB native libs, as that's normally done from within
        // static initializers which already ran during build
        recorder.loadRocksDb();
    }

    @BuildStep
    void addHealthChecks(KafkaStreamsBuildTimeConfig buildTimeConfig, BuildProducer<HealthBuildItem> healthChecks) {
        healthChecks.produce(
                new HealthBuildItem(
                        "io.quarkus.kafka.streams.runtime.health.KafkaStreamsTopicsHealthCheck",
                        buildTimeConfig.healthEnabled));
        healthChecks.produce(
                new HealthBuildItem(
                        "io.quarkus.kafka.streams.runtime.health.KafkaStreamsStateHealthCheck",
                        buildTimeConfig.healthEnabled));
    }
}
