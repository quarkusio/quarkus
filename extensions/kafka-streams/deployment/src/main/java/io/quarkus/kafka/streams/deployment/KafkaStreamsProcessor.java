package io.quarkus.kafka.streams.deployment;

import static io.quarkus.kafka.streams.runtime.KafkaStreamsPropertiesUtil.buildKafkaStreamsProperties;

import java.io.IOException;
import java.util.Properties;

import jakarta.inject.Singleton;

import org.apache.kafka.common.serialization.Serdes.ByteArraySerde;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.apache.kafka.streams.processor.FailOnInvalidTimestamp;
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier;
import org.apache.kafka.streams.processor.internals.StreamsPartitionAssignor;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;
import org.rocksdb.util.Environment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
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
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.kafka.streams.runtime.KafkaStreamsProducer;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRecorder;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsSupport;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class KafkaStreamsProcessor {

    public static final String DEFAULT_PARTITION_GROUPER = "org.apache.kafka.streams.processor.DefaultPartitionGrouper";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.KAFKA_STREAMS);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses,
            BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            LaunchModeBuildItem launchMode,
            NativeImageRunnerBuildItem nativeImageRunner) throws IOException {
        registerClassesThatAreLoadedThroughReflection(reflectiveClasses, launchMode);
        registerClassesThatAreAccessedViaJni(jniRuntimeAccessibleClasses);
        addSupportForRocksDbLib(nativeLibs, nativeImageRunner);
        enableLoadOfNativeLibs(reinitialized);
    }

    private void registerClassesThatAreLoadedThroughReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode) {
        registerCompulsoryClasses(reflectiveClasses);
        registerClassesThatClientMaySpecify(reflectiveClasses, launchMode);
    }

    private void registerCompulsoryClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(StreamsPartitionAssignor.class)
                .build());
        if (QuarkusClassLoader.isClassPresentAtRuntime(DEFAULT_PARTITION_GROUPER)) {
            // Class DefaultPartitionGrouper deprecated in Kafka 2.8.x and removed in 3.0.0
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(DEFAULT_PARTITION_GROUPER)
                            .build());
        }
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(DefaultKafkaClientSupplier.class)
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(DefaultProductionExceptionHandler.class)
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(FailOnInvalidTimestamp.class)
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(org.apache.kafka.streams.processor.internals.assignment.HighAvailabilityTaskAssignor.class)
                .methods().fields().build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(org.apache.kafka.streams.processor.internals.assignment.StickyTaskAssignor.class)
                .methods().fields().build());
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(org.apache.kafka.streams.processor.internals.assignment.FallbackPriorTaskAssignor.class)
                .methods().fields().build());
        // See https://github.com/quarkusio/quarkus/issues/23404
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("org.apache.kafka.streams.processor.internals.StateDirectory$StateDirectoryProcessFile")
                .methods().fields().build());
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
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(LogAndFailExceptionHandler.class)
                .build());
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

    private void addSupportForRocksDbLib(BuildProducer<NativeImageResourceBuildItem> nativeLibs,
            NativeImageRunnerBuildItem nativeImageRunnerFactory) {
        // for RocksDB, either add linux64 native lib when targeting containers
        if (nativeImageRunnerFactory.isContainerBuild()) {
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
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(defaultKeySerdeClass).build());
    }

    private boolean allDefaultSerdesAreDefinedInProperties(String defaultKeySerdeClass, String defaultValueSerdeClass) {
        return defaultKeySerdeClass != null && defaultValueSerdeClass != null;
    }

    private void registerDefaultSerde(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(ByteArraySerde.class).build());
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

        // make the producer an unremovable bean
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
