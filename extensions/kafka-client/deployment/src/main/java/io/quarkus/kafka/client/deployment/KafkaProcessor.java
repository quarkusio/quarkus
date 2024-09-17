package io.quarkus.kafka.client.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.security.auth.spi.LoginModule;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.authenticator.AbstractLogin;
import org.apache.kafka.common.security.authenticator.DefaultLogin;
import org.apache.kafka.common.security.authenticator.SaslClientCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.internals.OAuthBearerRefreshingLogin;
import org.apache.kafka.common.security.oauthbearer.internals.OAuthBearerSaslClient;
import org.apache.kafka.common.security.scram.internals.ScramSaslClient;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.ByteBufferDeserializer;
import org.apache.kafka.common.serialization.ByteBufferSerializer;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.DoubleDeserializer;
import org.apache.kafka.common.serialization.DoubleSerializer;
import org.apache.kafka.common.serialization.FloatDeserializer;
import org.apache.kafka.common.serialization.FloatSerializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.ShortDeserializer;
import org.apache.kafka.common.serialization.ShortSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.KafkaBindingConverter;
import io.quarkus.kafka.client.runtime.KafkaRecorder;
import io.quarkus.kafka.client.runtime.KafkaRuntimeConfigProducer;
import io.quarkus.kafka.client.runtime.SnappyRecorder;
import io.quarkus.kafka.client.runtime.devui.KafkaTopicClient;
import io.quarkus.kafka.client.runtime.devui.KafkaUiUtils;
import io.quarkus.kafka.client.serialization.BufferDeserializer;
import io.quarkus.kafka.client.serialization.BufferSerializer;
import io.quarkus.kafka.client.serialization.JsonArrayDeserializer;
import io.quarkus.kafka.client.serialization.JsonArraySerializer;
import io.quarkus.kafka.client.serialization.JsonObjectDeserializer;
import io.quarkus.kafka.client.serialization.JsonObjectSerializer;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.kafka.client.tls.QuarkusKafkaSslEngineFactory;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

public class KafkaProcessor {

    static final Class<?>[] BUILT_INS = {
            //serializers
            ShortSerializer.class,
            DoubleSerializer.class,
            LongSerializer.class,
            BytesSerializer.class,
            ByteArraySerializer.class,
            IntegerSerializer.class,
            ByteBufferSerializer.class,
            StringSerializer.class,
            FloatSerializer.class,
            // Provided in extension
            JsonObjectSerializer.class,
            JsonArraySerializer.class,
            BufferSerializer.class,

            //deserializers
            ShortDeserializer.class,
            DoubleDeserializer.class,
            LongDeserializer.class,
            BytesDeserializer.class,
            ByteArrayDeserializer.class,
            IntegerDeserializer.class,
            ByteBufferDeserializer.class,
            StringDeserializer.class,
            FloatDeserializer.class,
            // Provided in extension
            JsonObjectDeserializer.class,
            JsonArrayDeserializer.class,
            BufferDeserializer.class
    };

    static final DotName OBJECT_MAPPER = DotName.createSimple("com.fasterxml.jackson.databind.ObjectMapper");
    private static final Set<String> SASL_PROVIDERS = Arrays.stream(new String[] {
            "com.sun.security.sasl.Provider",
            "org.apache.kafka.common.security.scram.internals.ScramSaslClientProvider",
            "org.apache.kafka.common.security.oauthbearer.internals.OAuthBearerSaslClientProvider"
    }).collect(Collectors.toSet());

    private static final DotName LOGIN_MODULE = DotName.createSimple(LoginModule.class.getName());
    private static final DotName AUTHENTICATE_CALLBACK_HANDLER = DotName
            .createSimple(AuthenticateCallbackHandler.class.getName());

    static final DotName PARTITION_ASSIGNER = DotName
            .createSimple("org.apache.kafka.clients.consumer.internals.PartitionAssignor");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.KAFKA_CLIENT);
    }

    @BuildStep
    void logging(BuildProducer<LogCategoryBuildItem> log) {
        // Reduce the log level of Kafka as it tends to log a bit too much.
        // See - https://github.com/quarkusio/quarkus/issues/20170
        log.produce(new LogCategoryBuildItem("org.apache.kafka.clients", Level.WARNING));
        log.produce(new LogCategoryBuildItem("org.apache.kafka.common.utils", Level.WARNING));
        log.produce(new LogCategoryBuildItem("org.apache.kafka.common.metrics", Level.WARNING));
        log.produce(new LogCategoryBuildItem("org.apache.kafka.common.telemetry", Level.WARNING));
    }

    @BuildStep
    void silenceUnwantedConfigLogs(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilters) {
        String[] ignoredConfigProperties = { "wildfly.sasl.relax-compliance", "ssl.endpoint.identification.algorithm" };

        List<String> ignoredMessages = new ArrayList<>();
        for (String ignoredConfigProperty : ignoredConfigProperties) {
            ignoredMessages
                    .add("These configurations '[" + ignoredConfigProperty + "]' were supplied but are not used yet.");
        }

        logCleanupFilters.produce(new LogCleanupFilterBuildItem("org.apache.kafka.clients.consumer.ConsumerConfig",
                ignoredMessages));
        logCleanupFilters.produce(new LogCleanupFilterBuildItem("org.apache.kafka.clients.producer.ProducerConfig",
                ignoredMessages));
    }

    @BuildStep
    void addSaslProvidersToNativeImage(BuildProducer<NativeImageSecurityProviderBuildItem> additionalProviders) {
        for (String provider : SASL_PROVIDERS) {
            additionalProviders.produce(new NativeImageSecurityProviderBuildItem(provider));
        }
    }

    @BuildStep
    void contributeClassesToIndex(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClasses,
            BuildProducer<IndexDependencyBuildItem> indexDependency, Capabilities capabilities) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "kafka-clients"));
        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(
                JsonObjectSerializer.class.getName(),
                JsonObjectDeserializer.class.getName(),
                JsonArraySerializer.class.getName(),
                JsonArrayDeserializer.class.getName(),
                BufferSerializer.class.getName(),
                BufferDeserializer.class.getName(),
                ObjectMapperSerializer.class.getName(),
                ObjectMapperDeserializer.class.getName()));
        if (capabilities.isPresent(Capability.JSONB)) {
            additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(
                    JsonbSerializer.class.getName(), JsonbDeserializer.class.getName()));
        }
    }

    @BuildStep
    void relaxSaslElytron(BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {
        // If elytron is on the classpath and the Kafka connection uses SASL, the Elytron client SASL implementation
        // is stricter than what Kafka expects. In this case, configure the SASL client to relax some constraints.
        // See https://github.com/quarkusio/quarkus/issues/20088.
        if (!QuarkusClassLoader.isClassPresentAtRuntime("org.wildfly.security.sasl.gssapi.AbstractGssapiMechanism")) {
            return;
        }

        config.produce(new RunTimeConfigurationDefaultBuildItem("kafka.wildfly.sasl.relax-compliance", "true"));
    }

    @BuildStep
    public void build(
            KafkaBuildTimeConfig config, CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ConfigDescriptionBuildItem> configDescBuildItems,
            CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            Capabilities capabilities,
            BuildProducer<UnremovableBeanBuildItem> beans,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        final Set<DotName> toRegister = new HashSet<>();

        collectImplementors(toRegister, indexBuildItem, Serializer.class);
        collectImplementors(toRegister, indexBuildItem, Deserializer.class);
        collectImplementors(toRegister, indexBuildItem, Partitioner.class);
        if (QuarkusClassLoader.isClassPresentAtRuntime(PARTITION_ASSIGNER.toString())) {
            // PartitionAssignor is now deprecated, replaced by ConsumerPartitionAssignor
            collectImplementors(toRegister, indexBuildItem, PARTITION_ASSIGNER);
        }
        collectImplementors(toRegister, indexBuildItem, ConsumerPartitionAssignor.class);
        collectImplementors(toRegister, indexBuildItem, ConsumerInterceptor.class);
        collectImplementors(toRegister, indexBuildItem, ProducerInterceptor.class);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(OAuthBearerSaslClient.class,
                OAuthBearerSaslClient.OAuthBearerSaslClientFactory.class,
                OAuthBearerToken.class,
                OAuthBearerRefreshingLogin.class)
                .reason(getClass().getName() + " OAuthBearerSaslClient classes")
                .build());

        for (Class<?> i : BUILT_INS) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(i.getName())
                    .reason(getClass().getName() + " (de)serialization built-ins")
                    .build());
            collectSubclasses(toRegister, indexBuildItem, i);
        }

        // Kafka requires Jackson as it uses Jackson to handle authentication and some JSON utilities.
        // See https://github.com/quarkusio/quarkus/issues/16769
        // So, enable the Jackson support unconditionally.
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(ObjectMapperSerializer.class,
                        ObjectMapperDeserializer.class)
                        .reason(getClass().getName() + " Jackson support")
                        .build());
        collectSubclasses(toRegister, indexBuildItem, ObjectMapperSerializer.class);
        collectSubclasses(toRegister, indexBuildItem, ObjectMapperDeserializer.class);

        // Make the `io.quarkus.jackson.runtime.ObjectMapperProducer` bean cannot be removed.
        beans.produce(UnremovableBeanBuildItem.beanTypes(OBJECT_MAPPER));

        if (capabilities.isPresent(Capability.JSONB)) {
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(JsonbSerializer.class, JsonbDeserializer.class)
                            .reason(getClass().getName() + " " + Capability.JSONB + " support")
                            .build());
            collectSubclasses(toRegister, indexBuildItem, JsonbSerializer.class);
            collectSubclasses(toRegister, indexBuildItem, JsonbDeserializer.class);
        }

        for (DotName s : toRegister) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(s.toString())
                    .reason(getClass().getName() + " Jackson and " + Capability.JSONB + " support")
                    .build());
        }

        // built in partitioner and partition assignors
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                DefaultPartitioner.class,
                RangeAssignor.class,
                RoundRobinAssignor.class,
                StickyAssignor.class)
                .reason(getClass().getName() + " built-in partitioner and partition assignors")
                .build());

        handleAvro(reflectiveClass, proxies, serviceProviders, sslNativeSupport, capabilities);

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(QuarkusKafkaSslEngineFactory.class).build());
        configDescBuildItems.produce(new ConfigDescriptionBuildItem("kafka.tls-configuration-name", null,
                "The tls-configuration to use for the Kafka client", null, null, ConfigPhase.RUN_TIME));
    }

    @BuildStep(onlyIf = { HasSnappy.class, NativeOrNativeSourcesBuild.class })
    public void handleSnappyInNative(NativeImageRunnerBuildItem nativeImageRunner,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs) {
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("org.xerial.snappy.SnappyInputStream",
                "org.xerial.snappy.SnappyOutputStream")
                .reason(getClass().getName() + " snappy support")
                .methods().fields().build());

        String root = "org/xerial/snappy/native/";
        // add linux64 native lib when targeting containers
        if (nativeImageRunner.isContainerBuild()) {
            String dir = "Linux/x86_64";
            String snappyNativeLibraryName = "libsnappyjava.so";
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        } else { // otherwise the native lib of the platform this build runs on
            String dir = SnappyUtils.getNativeLibFolderPathForCurrentOS();
            String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        }
    }

    @BuildStep(onlyIf = HasSnappy.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadSnappyIfEnabled(LaunchModeBuildItem launch, SnappyRecorder recorder, KafkaBuildTimeConfig config) {
        boolean loadFromSharedClassLoader = false;
        if (launch.isTest()) {
            loadFromSharedClassLoader = config.snappyLoadFromSharedClassLoader;
        }
        recorder.loadSnappy(loadFromSharedClassLoader);
    }

    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @BuildStep(onlyIf = IsNormal.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void checkBoostrapServers(KafkaRecorder recorder, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            recorder.checkBoostrapServers();
        }
    }

    private void handleAvro(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            Capabilities capabilities) {
        // Avro - for both Confluent and Apicurio

        // --- Confluent ---
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.confluent.kafka.serializers.KafkaAvroDeserializer")
                && !capabilities.isPresent(Capability.CONFLUENT_REGISTRY_AVRO)) {
            throw new RuntimeException(
                    "Confluent Avro classes detected, please use the quarkus-confluent-registry-avro extension");
        }

        // --- Apicurio Registry 1.x ---
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.apicurio.registry.utils.serde.AvroKafkaDeserializer")) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                    "io.apicurio.registry.utils.serde.AvroKafkaDeserializer",
                    "io.apicurio.registry.utils.serde.AvroKafkaSerializer",
                    "io.apicurio.registry.utils.serde.avro.DefaultAvroDatumProvider",
                    "io.apicurio.registry.utils.serde.avro.ReflectAvroDatumProvider",
                    "io.apicurio.registry.utils.serde.strategy.AutoRegisterIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.CachedSchemaIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.FindBySchemaIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.FindLatestIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.GetOrCreateIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.RecordIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.TopicIdStrategy",
                    "io.apicurio.registry.utils.serde.strategy.TopicRecordIdStrategy")
                    .reason(getClass().getName() + " apicurio registry 1.x support")
                    .methods().build());

            // Apicurio uses dynamic proxies, register them
            proxies.produce(new NativeImageProxyDefinitionBuildItem("io.apicurio.registry.client.RegistryService",
                    "java.lang.AutoCloseable"));
        }

        // --- Apicurio Registry 2.x Avro ---
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.apicurio.registry.serde.avro.AvroKafkaDeserializer")
                && !capabilities.isPresent(Capability.APICURIO_REGISTRY_AVRO)) {
            throw new RuntimeException(
                    "Apicurio Registry 2.x Avro classes detected, please use the quarkus-apicurio-registry-avro extension");
        }

        // --- Apicurio Registry 2.x Json Schema ---
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.apicurio.registry.serde.avro.JsonKafkaDeserializer")
                && !capabilities.isPresent(Capability.APICURIO_REGISTRY_JSON_SCHEMA)) {
            throw new RuntimeException(
                    "Apicurio Registry 2.x Json classes detected, please use the quarkus-apicurio-registry-json extension");
        }
    }

    @BuildStep
    public AdditionalBeanBuildItem runtimeConfig() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(KafkaRuntimeConfigProducer.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    public void withSasl(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveClassConditionBuildItem> reflectiveClassCondition,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(
                        AbstractLogin.DefaultLoginCallbackHandler.class,
                        SaslClientCallbackHandler.class,
                        DefaultLogin.class,
                        ScramSaslClient.ScramSaslClientFactory.class)
                        .reason(getClass().getName() + " sasl support")
                        .build());

        // Enable SSL support if kafka.security.protocol is set to something other than PLAINTEXT, which is the default
        String securityProtocol = ConfigProvider.getConfig().getConfigValue("kafka.security.protocol").getValue();
        if (securityProtocol != null && SecurityProtocol.forName(securityProtocol) != SecurityProtocol.PLAINTEXT) {
            sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.KAFKA_CLIENT));
        }

        for (ClassInfo loginModule : index.getIndex().getAllKnownImplementors(LOGIN_MODULE)) {
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(loginModule.name().toString())
                            .reason(getClass().getName() + " sasl support " + LOGIN_MODULE + " known implementors")
                            .build());
        }
        // Kafka oauth login internally iterates over all ServiceLoader available LoginModule's
        registerJDKLoginModules(reflectiveClass);
        for (ClassInfo authenticateCallbackHandler : index.getIndex().getAllKnownImplementors(AUTHENTICATE_CALLBACK_HANDLER)) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(authenticateCallbackHandler.name().toString())
                    .reason(getClass().getName() + " sasl support " + AUTHENTICATE_CALLBACK_HANDLER
                            + " known implementors")
                    .build());
        }
        // Add a condition for the optional authenticate callback handler
        reflectiveClassCondition.produce(new ReflectiveClassConditionBuildItem(
                "org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerValidatorCallbackHandler",
                "org.jose4j.keys.resolvers.VerificationKeyResolver"));
        reflectiveClassCondition.produce(new ReflectiveClassConditionBuildItem(
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler",
                "org.jose4j.keys.resolvers.VerificationKeyResolver"));
    }

    private void registerJDKLoginModules(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // jdk.security.auth module provided LoginModule's
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.sun.security.auth.module.Krb5LoginModule",
                "com.sun.security.auth.module.UnixLoginModule",
                "com.sun.security.auth.module.JndiLoginModule",
                "com.sun.security.auth.module.KeyStoreLoginModule",
                "com.sun.security.auth.module.LdapLoginModule",
                "com.sun.security.auth.module.NTLoginModule",
                "com.sun.jmx.remote.security.FileLoginModule")
                .reason(getClass().getName() + " jdk.security.auth module provided LoginModule's")
                .build());
    }

    private static void collectImplementors(Set<DotName> set, CombinedIndexBuildItem indexBuildItem, Class<?> cls) {
        collectClassNames(set, indexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(cls.getName())));
    }

    private static void collectImplementors(Set<DotName> set, CombinedIndexBuildItem indexBuildItem, DotName className) {
        collectClassNames(set, indexBuildItem.getIndex().getAllKnownImplementors(className));
    }

    private static void collectSubclasses(Set<DotName> set, CombinedIndexBuildItem indexBuildItem, Class<?> cls) {
        collectClassNames(set, indexBuildItem.getIndex().getAllKnownSubclasses(DotName.createSimple(cls.getName())));
    }

    private static void collectClassNames(Set<DotName> set, Collection<ClassInfo> classInfos) {
        classInfos.forEach(new Consumer<ClassInfo>() {
            @Override
            public void accept(ClassInfo c) {
                set.add(c.name());
            }
        });
    }

    @BuildStep
    HealthBuildItem addHealthCheck(KafkaBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.kafka.client.health.KafkaHealthCheck",
                buildTimeConfig.healthEnabled);
    }

    @BuildStep
    UnremovableBeanBuildItem ensureJsonParserAvailable() {
        return UnremovableBeanBuildItem.beanClassNames(
                "io.quarkus.jackson.ObjectMapperProducer",
                "com.fasterxml.jackson.databind.ObjectMapper",
                "io.quarkus.jsonb.JsonbProducer",
                "jakarta.json.bind.Jsonb");
    }

    @BuildStep
    NativeImageConfigBuildItem nativeImageConfiguration() {
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder()
                // Classes using java.util.Random, which need to be runtime initialized
                .addRuntimeInitializedClass("org.apache.kafka.common.security.authenticator.SaslClientAuthenticator")
                .addRuntimeInitializedClass(
                        "org.apache.kafka.common.security.oauthbearer.internals.expiring.ExpiringCredentialRefreshingLogin")
                // VerificationKeyResolver is value on static map in OAuthBearerValidatorCallbackHandler
                .addRuntimeInitializedClass("org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler")
                .addRuntimeReinitializedClass("org.apache.kafka.shaded.com.google.protobuf.UnsafeUtil");
        return builder.build();
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem("io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter",
                            KafkaBindingConverter.class.getName()));
        }
    }

    @BuildStep
    public AdditionalBeanBuildItem kafkaAdmin() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(KafkaAdminClient.class)
                .setUnremovable()
                .build();
    }

    // Kafka UI related stuff

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem kafkaClientBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(KafkaTopicClient.class)
                .addBeanClass(KafkaUiUtils.class)
                .setUnremovable()
                .build();
    }

    public static final class HasSnappy implements BooleanSupplier {

        private final KafkaBuildTimeConfig config;

        public HasSnappy(KafkaBuildTimeConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime("org.xerial.snappy.OSInfo") && config.snappyEnabled;
        }
    }

}
