package io.quarkus.kafka.client.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.security.auth.spi.LoginModule;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerPartitionAssignor;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.clients.consumer.internals.PartitionAssignor;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
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
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.xerial.snappy.OSInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
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
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.kafka.client.runtime.KafkaBindingConverter;
import io.quarkus.kafka.client.runtime.KafkaRecorder;
import io.quarkus.kafka.client.runtime.KafkaRuntimeConfigProducer;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
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

            //deserializers
            ShortDeserializer.class,
            DoubleDeserializer.class,
            LongDeserializer.class,
            BytesDeserializer.class,
            ByteArrayDeserializer.class,
            IntegerDeserializer.class,
            ByteBufferDeserializer.class,
            StringDeserializer.class,
            FloatDeserializer.class
    };

    static final DotName OBJECT_MAPPER = DotName.createSimple("com.fasterxml.jackson.databind.ObjectMapper");
    private static final Set<String> SASL_PROVIDERS = Arrays.stream(new String[] {
            "com.sun.security.sasl.Provider",
            "org.apache.kafka.common.security.scram.internals.ScramSaslClientProvider",
            "org.apache.kafka.common.security.oauthbearer.internals.OAuthBearerSaslClientProvider"
    }).collect(Collectors.toSet());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.KAFKA_CLIENT);
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
        // This is needed for SASL authentication

        additionalIndexedClasses.produce(new AdditionalIndexedClassesBuildItem(
                LoginModule.class.getName(),
                javax.security.auth.Subject.class.getName(),
                javax.security.auth.login.AppConfigurationEntry.class.getName(),
                javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.class.getName()));

        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "kafka-clients"));
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
        collectImplementors(toRegister, indexBuildItem, PartitionAssignor.class);
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
        if (nativeConfig.containerRuntime.isPresent() || nativeConfig.containerBuild) {
            String dir = "Linux/x86_64";
            String snappyNativeLibraryName = "libsnappyjava.so";
            String path = root + dir + "/" + snappyNativeLibraryName;
            nativeLibs.produce(new NativeImageResourceBuildItem(path));
        } else { // otherwise the native lib of the platform this build runs on
            String dir = OSInfo.getNativeLibFolderPathForCurrentOS();
            String snappyNativeLibraryName = System.mapLibraryName("snappyjava");
            if (snappyNativeLibraryName.toLowerCase().endsWith(".dylib")) {
                snappyNativeLibraryName = snappyNativeLibraryName.replace(".dylib", ".jnilib");
            }
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
        if (capabilities.isPresent(Capability.OPENTRACING)) {
            try {
                Class.forName("io.opentracing.contrib.kafka.TracingProducerInterceptor", false,
                        Thread.currentThread().getContextClassLoader());
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, false,
                        "io.opentracing.contrib.kafka.TracingProducerInterceptor",
                        "io.opentracing.contrib.kafka.TracingConsumerInterceptor"));
            } catch (ClassNotFoundException e) {
                //ignore, opentracing contrib kafka is not in the classpath
            }
        }
    }

    private void handleStrimziOAuth(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        try {
            Class.forName("io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler", false,
                    Thread.currentThread().getContextClassLoader());

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
        } catch (ClassNotFoundException e) {
            //ignore, Strimzi OAuth Client is not on the classpath
        }
    }

    private void handleAvro(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            Capabilities capabilities) {
        // Avro - for both Confluent and Apicurio

        // --- Confluent ---
        try {
            Class.forName("io.confluent.kafka.serializers.KafkaAvroDeserializer", false,
                    Thread.currentThread().getContextClassLoader());
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false,
                            "io.confluent.kafka.serializers.KafkaAvroDeserializer",
                            "io.confluent.kafka.serializers.KafkaAvroSerializer"));

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, false,
                            "io.confluent.kafka.serializers.subject.TopicNameStrategy",
                            "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy",
                            "io.confluent.kafka.serializers.subject.RecordNameStrategy"));

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, false,
                            "io.confluent.kafka.schemaregistry.client.rest.entities.ErrorMessage",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.Schema",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.Config",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaTypeConverter",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.ServerClusterId",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.SujectVersion"));

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, false,
                            "io.confluent.kafka.schemaregistry.client.rest.entities.requests.CompatibilityCheckResponse",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ModeGetResponse",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ModeUpdateRequest",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaRequest",
                            "io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse"));
        } catch (ClassNotFoundException e) {
            //ignore, Confluent Avro is not in the classpath
        }

        try {
            Class.forName("io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider", false,
                    Thread.currentThread().getContextClassLoader());
            serviceProviders
                    .produce(new ServiceProviderBuildItem(
                            "io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider",
                            "io.confluent.kafka.schemaregistry.client.security.basicauth.SaslBasicAuthCredentialProvider",
                            "io.confluent.kafka.schemaregistry.client.security.basicauth.UrlBasicAuthCredentialProvider",
                            "io.confluent.kafka.schemaregistry.client.security.basicauth.UserInfoCredentialProvider"));
        } catch (ClassNotFoundException e) {
            // ignore, Confluent schema registry client not in the classpath
        }

        // --- Apicurio Registry 1.x ---
        try {
            Class.forName("io.apicurio.registry.utils.serde.AvroKafkaDeserializer", false,
                    Thread.currentThread().getContextClassLoader());
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

        } catch (ClassNotFoundException e) {
            // ignore, Apicurio Avro is not in the classpath
        }

        // --- Apicurio Registry 2.x ---
        try {
            Class.forName("io.apicurio.registry.serde.avro.AvroKafkaDeserializer", false,
                    Thread.currentThread().getContextClassLoader());

            if (!capabilities.isPresent(Capability.APICURIO_REGISTRY_AVRO)) {
                throw new RuntimeException(
                        "Apicurio Registry 2.x Avro classes detected, please use the quarkus-apicurio-registry-avro extension");
            }
        } catch (ClassNotFoundException e) {
            // ignore, Apicurio Avro is not in the classpath
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
    public void withSasl(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
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

        final Type loginModuleType = Type
                .create(DotName.createSimple(LoginModule.class.getName()), Kind.CLASS);

        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem.Builder()
                .type(loginModuleType)
                .source(getClass().getSimpleName() + " > " + loginModuleType.name().toString())
                .build());
    }

    private static void collectImplementors(Set<DotName> set, CombinedIndexBuildItem indexBuildItem, Class<?> cls) {
        collectClassNames(set, indexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(cls.getName())));
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
                "javax.json.bind.Jsonb");
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
}
