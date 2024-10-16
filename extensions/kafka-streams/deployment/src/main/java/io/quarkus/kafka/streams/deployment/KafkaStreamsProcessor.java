package io.quarkus.kafka.streams.deployment;

import static io.quarkus.kafka.streams.runtime.KafkaStreamsPropertiesUtil.buildKafkaStreamsProperties;

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
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.kafka.streams.runtime.KafkaStreamsProducer;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRecorder;
import io.quarkus.kafka.streams.runtime.KafkaStreamsRuntimeConfig;
import io.quarkus.kafka.streams.runtime.KafkaStreamsSupport;
import io.quarkus.kafka.streams.runtime.graal.KafkaStreamsFeature;
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
            LaunchModeBuildItem launchMode) {
        registerClassesThatAreLoadedThroughReflection(reflectiveClasses, launchMode);
        registerClassesThatAreAccessedViaJni(jniRuntimeAccessibleClasses);
        enableLoadOfNativeLibs(reinitialized);
    }

    private void registerClassesThatAreLoadedThroughReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode) {
        registerCompulsoryClasses(reflectiveClasses);
        registerClassesThatClientMaySpecify(reflectiveClasses, launchMode);
    }

    private void registerCompulsoryClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(StreamsPartitionAssignor.class)
                .reason(getClass().getName())
                .build());
        if (QuarkusClassLoader.isClassPresentAtRuntime(DEFAULT_PARTITION_GROUPER)) {
            // Class DefaultPartitionGrouper deprecated in Kafka 2.8.x and removed in 3.0.0
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(DEFAULT_PARTITION_GROUPER)
                            .reason(getClass().getName() + " compulsory class")
                            .build());
        }
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                DefaultKafkaClientSupplier.class,
                DefaultProductionExceptionHandler.class,
                FailOnInvalidTimestamp.class)
                .reason(getClass().getName())
                .build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                org.apache.kafka.streams.processor.internals.assignment.HighAvailabilityTaskAssignor.class,
                org.apache.kafka.streams.processor.internals.assignment.StickyTaskAssignor.class,
                org.apache.kafka.streams.processor.internals.assignment.FallbackPriorTaskAssignor.class)
                .reason(getClass().getName())
                .methods().fields().build());
        // See https://github.com/quarkusio/quarkus/issues/23404
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("org.apache.kafka.streams.processor.internals.StateDirectory$StateDirectoryProcessFile")
                .reason(getClass().getName())
                .methods().fields().build());

        // Listed in BuiltInDslStoreSuppliers
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(org.apache.kafka.streams.state.BuiltInDslStoreSuppliers.RocksDBDslStoreSuppliers.class,
                        org.apache.kafka.streams.state.BuiltInDslStoreSuppliers.InMemoryDslStoreSuppliers.class)
                .reason(getClass().getName())
                .build());
    }

    private void registerClassesThatClientMaySpecify(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            LaunchModeBuildItem launchMode) {
        Properties properties = buildKafkaStreamsProperties(launchMode.getLaunchMode());
        registerExceptionHandler(reflectiveClasses, properties);
        registerDefaultSerdes(reflectiveClasses, properties);
        registerDslStoreSupplier(reflectiveClasses, properties);
    }

    private void registerDslStoreSupplier(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Properties kafkaStreamsProperties) {
        String dlsStoreSupplierClassName = kafkaStreamsProperties
                .getProperty(StreamsConfig.DSL_STORE_SUPPLIERS_CLASS_CONFIG);

        if (dlsStoreSupplierClassName != null) {
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(dlsStoreSupplierClassName)
                            .reason(getClass().getName())
                            .build());
        }
    }

    private void registerExceptionHandler(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Properties kafkaStreamsProperties) {
        String exceptionHandlerClassName = kafkaStreamsProperties
                .getProperty(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG);

        if (exceptionHandlerClassName == null) {
            registerDefaultExceptionHandler(reflectiveClasses);
        } else {
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(exceptionHandlerClassName)
                            .reason(getClass().getName())
                            .build());
        }
    }

    private void registerDefaultExceptionHandler(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(LogAndFailExceptionHandler.class)
                .reason(getClass().getName())
                .build());
    }

    private void registerDefaultSerdes(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Properties kafkaStreamsProperties) {
        String defaultKeySerdeClass = kafkaStreamsProperties.getProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG);
        String defaultValueSerdeClass = kafkaStreamsProperties.getProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG);

        if (defaultKeySerdeClass != null) {
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(defaultKeySerdeClass)
                            .reason(getClass().getName())
                            .build());
        }
        if (defaultValueSerdeClass != null) {
            reflectiveClasses.produce(
                    ReflectiveClassBuildItem.builder(defaultValueSerdeClass)
                            .reason(getClass().getName())
                            .build());
        }
        if (!allDefaultSerdesAreDefinedInProperties(defaultKeySerdeClass, defaultValueSerdeClass)) {
            registerDefaultSerde(reflectiveClasses);
        }
    }

    private void registerClassesThatAreAccessedViaJni(BuildProducer<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses) {
        jniRuntimeAccessibleClasses
                .produce(new JniRuntimeAccessBuildItem(true, false, false, RocksDBException.class, Status.class));
    }

    private void enableLoadOfNativeLibs(BuildProducer<RuntimeReinitializedClassBuildItem> reinitialized) {
        reinitialized.produce(new RuntimeReinitializedClassBuildItem("org.rocksdb.RocksDB"));
    }

    private boolean allDefaultSerdesAreDefinedInProperties(String defaultKeySerdeClass, String defaultValueSerdeClass) {
        return defaultKeySerdeClass != null && defaultValueSerdeClass != null;
    }

    private void registerDefaultSerde(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(ByteArraySerde.class).reason(getClass().getName()).build());
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

    @BuildStep
    NativeImageFeatureBuildItem kafkaStreamsFeature() {
        return new NativeImageFeatureBuildItem(KafkaStreamsFeature.class.getName());
    }
}
