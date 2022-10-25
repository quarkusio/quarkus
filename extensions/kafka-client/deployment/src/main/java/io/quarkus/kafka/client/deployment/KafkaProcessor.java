package io.quarkus.kafka.client.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.xerial.snappy.OSInfo;

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
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleWebjarBuildItem;
import io.quarkus.kafka.client.runtime.*;
import io.quarkus.kafka.client.runtime.KafkaRuntimeConfigProducer;
import io.quarkus.kafka.client.runtime.ui.KafkaTopicClient;
import io.quarkus.kafka.client.runtime.ui.KafkaUiRecorder;
import io.quarkus.kafka.client.runtime.ui.KafkaUiUtils;
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
import io.quarkus.maven.dependency.GACT;
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
    private static final GACT DEVCONSOLE_WEBJAR_ARTIFACT_KEY = new GACT("io.quarkus",
            "quarkus-kafka-client-deployment", null, "jar");
    private static final String DEVCONSOLE_WEBJAR_STATIC_RESOURCES_PATH = "dev-static/";
    public static final String KAFKA_ADMIN_PATH = "kafka-admin";
    public static final String KAFKA_RESOURCES_ROOT_PATH = "kafka-ui";

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
    }

    @BuildStep
    void silenceUnwantedConfigLogs(BuildProducer<LogCleanupFilterBuildItem> logCleanupFilters) {
        String[] ignoredConfigProperties = { "wildfly.sasl.relax-compliance", "ssl.endpoint.identification.algorithm" };

        List<String> ignoredMessages = new ArrayList<>();
        for (String ignoredConfigProperty : ignoredConfigProperties) {
            ignoredMessages
                    .add("The configuration '" + ignoredConfigProperty + "' was supplied but isn't a known config.");
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
            BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "kafka-clients"));
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
            KafkaBuildTimeConfig config,
            CombinedIndexBuildItem indexBuildItem, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            Capabilities capabilities, BuildProducer<UnremovableBeanBuildItem> beans,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs, NativeConfig nativeConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        final Set<DotName> toRegister = new HashSet<>();

        collectImplementors(toRegister, indexBuildItem, Serializer.class);
        collectImplementors(toRegister, indexBuildItem, Deserializer.class);
        collectImplementors(toRegister, indexBuildItem, Partitioner.class);
        // PartitionAssignor is now deprecated, replaced by ConsumerPartitionAssignor
        collectImplementors(toRegister, indexBuildItem, PARTITION_ASSIGNER);
        collectImplementors(toRegister, indexBuildItem, ConsumerPartitionAssignor.class);
        collectImplementors(toRegister, indexBuildItem, ConsumerInterceptor.class);
        collectImplementors(toRegister, indexBuildItem, ProducerInterceptor.class);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                OAuthBearerSaslClient.class,
                OAuthBearerSaslClient.OAuthBearerSaslClientFactory.class,
                OAuthBearerToken.class,
                OAuthBearerRefreshingLogin.class));

        for (Class<?> i : BUILT_INS) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, i.getName()));
            collectSubclasses(toRegister, indexBuildItem, i);
        }

        // Kafka requires Jackson as it uses Jackson to handle authentication and some JSON utilities.
        // See https://github.com/quarkusio/quarkus/issues/16769
        // So, enable the Jackson support unconditionally.
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(false, false, ObjectMapperSerializer.class,
                        ObjectMapperDeserializer.class));
        collectSubclasses(toRegister, indexBuildItem, ObjectMapperSerializer.class);
        collectSubclasses(toRegister, indexBuildItem, ObjectMapperDeserializer.class);

        // Make the `io.quarkus.jackson.runtime.ObjectMapperProducer` bean cannot be removed.
        beans.produce(UnremovableBeanBuildItem.beanTypes(OBJECT_MAPPER));

        if (capabilities.isPresent(Capability.JSONB)) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(false, false, JsonbSerializer.class, JsonbDeserializer.class));
            collectSubclasses(toRegister, indexBuildItem, JsonbSerializer.class);
            collectSubclasses(toRegister, indexBuildItem, JsonbDeserializer.class);
        }

        for (DotName s : toRegister) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, s.toString()));
        }

        // built in partitioner and partition assignors
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultPartitioner.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RangeAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, RoundRobinAssignor.class.getName()));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, StickyAssignor.class.getName()));

        // classes needed to perform reflection on DirectByteBuffer - only really needed for Java 8
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "java.nio.DirectByteBuffer"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "sun.misc.Cleaner"));

        handleAvro(reflectiveClass, proxies, serviceProviders, sslNativeSupport, capabilities);
        handleOpenTracing(reflectiveClass, capabilities);
        handleStrimziOAuth(reflectiveClass);
        if (config.snappyEnabled) {
            handleSnappy(reflectiveClass, nativeLibs, nativeConfig);
        }

    }

    private void handleSnappy(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeLibs, NativeConfig nativeConfig) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                "org.xerial.snappy.SnappyInputStream",
                "org.xerial.snappy.SnappyOutputStream"));

        String root = "org/xerial/snappy/native/";
        // add linux64 native lib when targeting containers
        if (nativeConfig.isContainerBuild()) {
            String dir = "Linux/x86_64";
            String snappyNativeLibraryName = "libsnappyjava.so";
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        } else { // otherwise the native lib of the platform this build runs on
            String dir = OSInfo.getNativeLibFolderPathForCurrentOS();
            String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadSnappyIfEnabled(KafkaRecorder recorder, KafkaBuildTimeConfig config) {
        if (config.snappyEnabled) {
            recorder.loadSnappy();
        }
    }

    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @BuildStep(onlyIf = IsNormal.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void checkBoostrapServers(KafkaRecorder recorder, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            recorder.checkBoostrapServers();
        }
    }

    private void handleOpenTracing(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, Capabilities capabilities) {
        //opentracing contrib kafka interceptors: https://github.com/opentracing-contrib/java-kafka-client
        if (!capabilities.isPresent(Capability.OPENTRACING)
                || !QuarkusClassLoader.isClassPresentAtRuntime("io.opentracing.contrib.kafka.TracingProducerInterceptor")) {
            return;
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
                "io.opentracing.contrib.kafka.TracingProducerInterceptor",
                "io.opentracing.contrib.kafka.TracingConsumerInterceptor"));
    }

    private void handleStrimziOAuth(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime("io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler")) {
            return;
        }

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, true,
                "org.keycloak.jose.jws.JWSHeader",
                "org.keycloak.representations.AccessToken",
                "org.keycloak.representations.AccessToken$Access",
                "org.keycloak.representations.AccessTokenResponse",
                "org.keycloak.representations.IDToken",
                "org.keycloak.representations.JsonWebToken",
                "org.keycloak.jose.jwk.JSONWebKeySet",
                "org.keycloak.jose.jwk.JWK",
                "org.keycloak.json.StringOrArrayDeserializer",
                "org.keycloak.json.StringListMapDeserializer"));
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
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, false,
                            "io.apicurio.registry.utils.serde.AvroKafkaDeserializer",
                            "io.apicurio.registry.utils.serde.AvroKafkaSerializer"));

            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
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
                    "io.apicurio.registry.utils.serde.strategy.TopicRecordIdStrategy"));

            // Apicurio uses dynamic proxies, register them
            proxies.produce(new NativeImageProxyDefinitionBuildItem("io.apicurio.registry.client.RegistryService",
                    "java.lang.AutoCloseable"));
        }

        // --- Apicurio Registry 2.x ---
        if (QuarkusClassLoader.isClassPresentAtRuntime("io.apicurio.registry.serde.avro.AvroKafkaDeserializer")
                && !capabilities.isPresent(Capability.APICURIO_REGISTRY_AVRO)) {
            throw new RuntimeException(
                    "Apicurio Registry 2.x Avro classes detected, please use the quarkus-apicurio-registry-avro extension");
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
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        reflectiveClass
                .produce(new ReflectiveClassBuildItem(false, false, AbstractLogin.DefaultLoginCallbackHandler.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, SaslClientCallbackHandler.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, DefaultLogin.class));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, false, false, ScramSaslClient.ScramSaslClientFactory.class));

        // Enable SSL support if kafka.security.protocol is set to something other than PLAINTEXT, which is the default
        String securityProtocol = ConfigProvider.getConfig().getConfigValue("kafka.security.protocol").getValue();
        if (securityProtocol != null && SecurityProtocol.forName(securityProtocol) != SecurityProtocol.PLAINTEXT) {
            sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.KAFKA_CLIENT));
        }

        for (ClassInfo loginModule : index.getIndex().getAllKnownImplementors(LOGIN_MODULE)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, loginModule.name().toString()));
        }
        for (ClassInfo authenticateCallbackHandler : index.getIndex().getAllKnownImplementors(AUTHENTICATE_CALLBACK_HANDLER)) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, authenticateCallbackHandler.name().toString()));
        }
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
    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> producer) {
        // Classes using java.util.Random, which need to be runtime initialized
        producer.produce(
                new RuntimeInitializedClassBuildItem("org.apache.kafka.common.security.authenticator.SaslClientAuthenticator"));
        producer.produce(new RuntimeInitializedClassBuildItem(
                "org.apache.kafka.common.security.oauthbearer.internals.expiring.ExpiringCredentialRefreshingLogin"));
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

    // Kafka UI related stuff

    @BuildStep
    public AdditionalBeanBuildItem kafkaClientBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(KafkaAdminClient.class)
                .addBeanClass(KafkaTopicClient.class)
                .addBeanClass(KafkaUiUtils.class)
                .setUnremovable()
                .build();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public void registerKafkaUiExecHandler(
            BuildProducer<DevConsoleRouteBuildItem> routeProducer,
            KafkaUiRecorder recorder) {
        routeProducer.produce(DevConsoleRouteBuildItem.builder()
                .method("POST")
                .handler(recorder.kafkaControlHandler())
                .path(KAFKA_ADMIN_PATH)
                .bodyHandlerRequired()
                .build());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleWebjarBuildItem setupWebJar(LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }
        return DevConsoleWebjarBuildItem.builder().artifactKey(DEVCONSOLE_WEBJAR_ARTIFACT_KEY)
                .root(DEVCONSOLE_WEBJAR_STATIC_RESOURCES_PATH)
                .routeRoot(KAFKA_RESOURCES_ROOT_PATH)
                .build();
    }

}
