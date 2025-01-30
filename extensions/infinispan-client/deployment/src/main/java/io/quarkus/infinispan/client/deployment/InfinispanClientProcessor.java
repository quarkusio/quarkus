package io.quarkus.infinispan.client.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.infinispan.client.runtime.InfinispanClientProducer.PROTOBUF_FILE_PREFIX;
import static io.quarkus.infinispan.client.runtime.InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.WrappedMessage;
import org.infinispan.protostream.schema.Schema;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.InjectionPointTransformerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.infinispan.client.Remote;
import io.quarkus.infinispan.client.runtime.InfinispanClientBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanClientProducer;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.infinispan.client.runtime.InfinispanClientsBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanRecorder;
import io.quarkus.infinispan.client.runtime.InfinispanServiceBindingConverter;
import io.quarkus.infinispan.client.runtime.cache.CacheInvalidateAllInterceptor;
import io.quarkus.infinispan.client.runtime.cache.CacheInvalidateInterceptor;
import io.quarkus.infinispan.client.runtime.cache.CacheResultInterceptor;
import io.quarkus.infinispan.client.runtime.cache.SynchronousInfinispanGet;
import io.quarkus.infinispan.client.runtime.graal.DisableLoggingFeature;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class InfinispanClientProcessor {
    private static final Log log = LogFactory.getLog(InfinispanClientProcessor.class);

    private static final String SERVICE_BINDING_INTERFACE_NAME = "io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter";
    private static final DotName INFINISPAN_CLIENT_ANNOTATION = DotName.createSimple(InfinispanClientName.class.getName());
    private static final DotName INFINISPAN_REMOTE_ANNOTATION = DotName.createSimple(Remote.class.getName());

    private static final DotName INFINISPAN_CLIENT = DotName.createSimple(RemoteCacheManager.class.getName());
    private static final DotName INFINISPAN_COUNTER_MANAGER = DotName.createSimple(CounterManager.class.getName());
    private static final DotName INFINISPAN_CACHE_CLIENT = DotName.createSimple(RemoteCache.class.getName());
    private static final String META_INF = "META-INF";
    private static final String DEFAULT_HOTROD_CLIENT_PROPERTIES = "hotrod-client.properties";
    private static final String PROTO_EXTENSION = ".proto";
    private static final String SASL_SECURITY_PROVIDER = "com.sun.security.sasl.Provider";

    private static final List<DotName> SUPPORTED_INJECTION_TYPE = List.of(
            // Client types
            INFINISPAN_CLIENT,
            INFINISPAN_COUNTER_MANAGER,
            INFINISPAN_CACHE_CLIENT);

    /**
     * The Infinispan client build time configuration.
     */
    InfinispanClientsBuildTimeConfig infinispanClientsBuildTimeConfig;

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }

    /**
     * Sets up additional properties for use when proto stream marshaller is in use
     */
    @BuildStep
    public void handleProtoStreamRequirements(BuildProducer<MarshallingBuildItem> protostreamPropertiesBuildItem)
            throws ClassNotFoundException {
        Properties properties = new Properties();
        Map<String, Object> marshallers = new HashMap<>();
        initMarshaller(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME,
                infinispanClientsBuildTimeConfig.defaultInfinispanClient().marshallerClass(), marshallers);
        for (String clientName : infinispanClientsBuildTimeConfig.getInfinispanNamedClientConfigNames()) {
            initMarshaller(clientName,
                    infinispanClientsBuildTimeConfig.getInfinispanClientBuildTimeConfig(clientName).marshallerClass(),
                    marshallers);
        }
        protostreamPropertiesBuildItem.produce(new MarshallingBuildItem(properties, marshallers));
    }

    private static void initMarshaller(String clientName, Optional<String> marshallerOpt, Map<String, Object> marshallers)
            throws ClassNotFoundException {

        if (marshallerOpt.isPresent()) {
            Class<?> marshallerClass = Class.forName(
                    marshallerOpt.get(), false,
                    Thread.currentThread().getContextClassLoader());
            marshallers.put(clientName, Util.getInstance(marshallerClass));
        } else {
            // Default to proto stream marshaller if one is not provided
            marshallers.put(clientName, new ProtoStreamMarshaller());
        }
    }

    @BuildStep
    InfinispanPropertiesBuildItem setup(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeployment,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<NativeImageSecurityProviderBuildItem> nativeImageSecurityProviders,
            BuildProducer<NativeImageConfigBuildItem> nativeImageConfig,
            BuildProducer<InfinispanClientNameBuildItem> infinispanClientNames,
            MarshallingBuildItem marshallingBuildItem,
            BuildProducer<NativeImageResourceBuildItem> resourceBuildItem,
            CombinedIndexBuildItem applicationIndexBuildItem) throws ClassNotFoundException, IOException {

        feature.produce(new FeatureBuildItem(Feature.INFINISPAN_CLIENT));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(InfinispanClientProducer.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CacheInvalidateAllInterceptor.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CacheResultInterceptor.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CacheInvalidateInterceptor.class));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(SynchronousInfinispanGet.class));
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(InfinispanClientName.class).build());
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(Remote.class).build());

        resourceBuildItem.produce(new NativeImageResourceBuildItem("proto/generated/query.proto"));
        resourceBuildItem.produce(new NativeImageResourceBuildItem(WrappedMessage.PROTO_FILE));
        hotDeployment
                .produce(new HotDeploymentWatchedFileBuildItem(META_INF + File.separator + DEFAULT_HOTROD_CLIENT_PROPERTIES));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.INFINISPAN_CLIENT));
        nativeImageSecurityProviders.produce(new NativeImageSecurityProviderBuildItem(SASL_SECURITY_PROVIDER));

        // add per cache file config
        handlePerCacheFileConfig(infinispanClientsBuildTimeConfig.defaultInfinispanClient(), resourceBuildItem, hotDeployment);
        for (InfinispanClientBuildTimeConfig config : infinispanClientsBuildTimeConfig.namedInfinispanClients().values()) {
            handlePerCacheFileConfig(config, resourceBuildItem, hotDeployment);
        }

        Map<String, Properties> propertiesMap = new HashMap<>();
        IndexView index = applicationIndexBuildItem.getIndex();

        // named and default
        Set<String> allClientNames = infinispanClientNames(applicationIndexBuildItem, infinispanClientNames);
        allClientNames.addAll(infinispanClientsBuildTimeConfig.getInfinispanNamedClientConfigNames());
        allClientNames.add(DEFAULT_INFINISPAN_CLIENT_NAME);
        for (String clientName : allClientNames) {
            Properties properties = loadHotrodProperties(clientName, reflectiveClass, marshallingBuildItem);
            propertiesMap.put(clientName, properties);

            // This is always non-null
            Object marshaller = properties.get(ConfigurationProperties.MARSHALLER);

            if (marshaller instanceof ProtoStreamMarshaller) {
                for (ApplicationArchive applicationArchive : applicationArchivesBuildItem.getAllApplicationArchives()) {
                    // If we have properties file we may have to care about
                    Path metaPath = applicationArchive.getChildPath(META_INF);

                    if (metaPath != null) {
                        try (Stream<Path> dirElements = Files.list(metaPath)) {
                            Iterator<Path> protoFiles = dirElements
                                    .filter(Files::isRegularFile)
                                    .filter(p -> p.toString().endsWith(PROTO_EXTENSION))
                                    .iterator();
                            // We monitor the entire meta inf directory if properties are available
                            if (protoFiles.hasNext()) {
                                // Quarkus doesn't currently support hot deployment watching directories
                                //                hotDeployment.produce(new HotDeploymentConfigFileBuildItem(META_INF));
                            }
                            while (protoFiles.hasNext()) {
                                Path path = protoFiles.next();
                                if (log.isDebugEnabled()) {
                                    log.debug("  " + path.toAbsolutePath());
                                }
                                byte[] bytes = Files.readAllBytes(path);
                                // This uses the default file encoding - should we enforce UTF-8?
                                properties.put(PROTOBUF_FILE_PREFIX + path.getFileName().toString(),
                                        new String(bytes, StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
                properties.putAll(marshallingBuildItem.getProperties());
                Collection<ClassInfo> initializerClasses = index.getAllKnownImplementors(DotName.createSimple(
                        SerializationContextInitializer.class.getName()));
                initializerClasses
                        .addAll(index.getAllKnownImplementors(DotName.createSimple(GeneratedSchema.class.getName())));

                Set<SerializationContextInitializer> initializers = new HashSet<>(initializerClasses.size());
                for (ClassInfo ci : initializerClasses) {
                    Class<?> initializerClass = Thread.currentThread().getContextClassLoader().loadClass(ci.toString());
                    try {
                        SerializationContextInitializer sci = (SerializationContextInitializer) initializerClass
                                .getDeclaredConstructor().newInstance();
                        initializers.add(sci);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                            | NoSuchMethodException e) {
                        // This shouldn't ever be possible as annotation processor should generate empty constructor
                        throw new RuntimeException(e);
                    }
                }
                if (!initializers.isEmpty()) {
                    properties.put(InfinispanClientProducer.PROTOBUF_INITIALIZERS, initializers);
                }
            }
        }

        // Add any user project listeners to allow reflection in native code
        Collection<AnnotationInstance> listenerInstances = index.getAnnotations(
                DotName.createSimple(ClientListener.class.getName()));
        for (AnnotationInstance instance : listenerInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                        target.asClass().name().toString())
                        .methods().build());
            }
        }

        // This is required for netty to work properly
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "io.netty.channel.socket.nio.NioSocketChannel").build());
        // We use reflection to have continuous queries work
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "org.infinispan.client.hotrod.event.impl.ContinuousQueryImpl$ClientEntryListener")
                .methods().build());
        // We use reflection to allow for near cache invalidations
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "org.infinispan.client.hotrod.near.NearCacheService$InvalidatedNearCacheListener")
                .methods().build());
        // This is required when a cache is clustered to tell us topology
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(
                        "org.infinispan.client.hotrod.impl.consistenthash.SegmentConsistentHash")
                        .build());

        // Elytron Classes
        String[] elytronClasses = new String[] {
                "org.wildfly.security.sasl.plain.PlainSaslClientFactory",
                "org.wildfly.security.sasl.scram.ScramSaslClientFactory",
                "org.wildfly.security.sasl.digest.DigestClientFactory",
                "org.wildfly.security.credential.BearerTokenCredential",
                "org.wildfly.security.credential.GSSKerberosCredential",
                "org.wildfly.security.credential.KeyPairCredential",
                "org.wildfly.security.credential.PasswordCredential",
                "org.wildfly.security.credential.PublicKeyCredential",
                "org.wildfly.security.credential.SecretKeyCredential",
                "org.wildfly.security.credential.SSHCredential",
                "org.wildfly.security.digest.SHA512_256MessageDigest",
                "org.wildfly.security.credential.X509CertificateChainPrivateCredential"
        };

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(elytronClasses).reason(getClass().getName()).build());
        return new InfinispanPropertiesBuildItem(propertiesMap);
    }

    private void handlePerCacheFileConfig(InfinispanClientBuildTimeConfig config,
            BuildProducer<NativeImageResourceBuildItem> resourceBuildItem,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeployment) {
        for (InfinispanClientBuildTimeConfig.RemoteCacheConfig cacheConfig : config.cache().values()) {
            if (cacheConfig.configurationResource().isPresent()) {
                resourceBuildItem.produce(new NativeImageResourceBuildItem(cacheConfig.configurationResource().get()));
                hotDeployment.produce(new HotDeploymentWatchedFileBuildItem(cacheConfig.configurationResource().get()));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem build(InfinispanRecorder recorder, InfinispanPropertiesBuildItem builderBuildItem) {
        Map<String, Properties> propertiesMap = builderBuildItem.getProperties();

        addMaxEntries(DEFAULT_INFINISPAN_CLIENT_NAME,
                infinispanClientsBuildTimeConfig.defaultInfinispanClient(), propertiesMap.get(DEFAULT_INFINISPAN_CLIENT_NAME));
        for (Map.Entry<String, InfinispanClientBuildTimeConfig> config : infinispanClientsBuildTimeConfig
                .namedInfinispanClients()
                .entrySet()) {
            addMaxEntries(config.getKey(), config.getValue(), propertiesMap.get(config.getKey()));
        }

        // This is necessary to be done for Protostream Marshaller init in native
        return new BeanContainerListenerBuildItem(recorder.configureInfinispan(propertiesMap));
    }

    /**
     * Reads all the contents of the file as a single string using default charset
     *
     * @param fileName file on class path to read contents of
     * @return string containing the contents of the file
     */
    private static String getContents(String fileName) {
        InputStream stream = InfinispanClientProducer.class.getResourceAsStream(fileName);
        return getContents(stream);
    }

    /**
     * Reads all the contents of the input stream as a single string using default charset
     *
     * @param stream to read contents of
     * @return string containing the contents of the file
     */
    private static String getContents(InputStream stream) {
        try (Scanner scanner = new Scanner(stream, "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    private Set<String> infinispanClientNames(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<InfinispanClientNameBuildItem> infinispanClientNames) {
        Set<String> clientNames = new HashSet<>();
        IndexView indexView = indexBuildItem.getIndex();
        // adds to clientNames all the client names scanned from @InfinispanClientName annotation
        Collection<AnnotationInstance> infinispanClientAnnotations = indexView.getAnnotations(INFINISPAN_CLIENT_ANNOTATION);
        for (AnnotationInstance annotation : infinispanClientAnnotations) {
            clientNames.add(annotation.value().asString());
        }
        // dev mode client name for default - 0 config
        if (infinispanClientsBuildTimeConfig.defaultInfinispanClient().devservices().devservices().enabled()
                && infinispanClientsBuildTimeConfig.defaultInfinispanClient().devservices().devservices()
                        .createDefaultClient()) {
            clientNames.add(DEFAULT_INFINISPAN_CLIENT_NAME);
        }

        for (String clientName : clientNames) {
            // Produce a client name for each client (produces later RemoteCacheManager and CounterManager instances)
            infinispanClientNames.produce(new InfinispanClientNameBuildItem(clientName));
        }
        return clientNames;
    }

    private Properties loadHotrodProperties(String clientName,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            MarshallingBuildItem marshallingBuildItem) {
        String filePath;
        if (InfinispanClientUtil.isDefault(clientName)) {
            filePath = META_INF + "/" + DEFAULT_HOTROD_CLIENT_PROPERTIES;
        } else {
            filePath = META_INF + "/" + clientName + "-" + DEFAULT_HOTROD_CLIENT_PROPERTIES;
        }
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(filePath);
        Properties properties;
        if (stream == null) {
            properties = new Properties();
            if (log.isTraceEnabled()) {
                log.tracef("There was no %s file found - using defaults", filePath);
            }
        } else {
            try {
                properties = loadFromStream(stream);
                if (log.isDebugEnabled()) {
                    log.debugf("Found %s properties of %s", filePath, properties);
                }
            } finally {
                Util.close(stream);
            }

            // We use caffeine for bounded near cache - so register that reflection if we have a bounded near cache
            if (properties.containsKey(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES)) {
                reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder("com.github.benmanes.caffeine.cache.SSMS").build());
                reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder("com.github.benmanes.caffeine.cache.PSMS").build());
            }
        }

        Object marshaller = marshallingBuildItem.getMarshallerForClientName(clientName);
        if (marshaller == null) {
            marshaller = new ProtoStreamMarshaller();
        }
        properties.put(ConfigurationProperties.MARSHALLER, marshaller);
        return properties;
    }

    private Properties loadFromStream(InputStream stream) {
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            throw new HotRodClientException("Issues configuring from client hotrod-client.properties", e);
        }
        return properties;
    }

    private void addMaxEntries(String clientName, InfinispanClientBuildTimeConfig config, Properties properties) {
        if (log.isDebugEnabled()) {
            log.debugf("Applying micro profile configuration: %s", config);
        }
        // Only write the entries if it is a valid number and it isn't already configured
        if (config.nearCacheMaxEntries() > 0 && !properties.containsKey(ConfigurationProperties.NEAR_CACHE_MODE)) {
            // This is already empty so no need for putIfAbsent
            if (InfinispanClientUtil.isDefault(clientName)) {
                properties.put(ConfigurationProperties.NEAR_CACHE_MODE, NearCacheMode.INVALIDATED.toString());
                properties.putIfAbsent(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES, config.nearCacheMaxEntries());
            }
        }
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return UnremovableBeanBuildItem.beanTypes(BaseMarshaller.class, EnumMarshaller.class, MessageMarshaller.class,
                FileDescriptorSource.class, Schema.class);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(InfinispanClientsBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.infinispan.client.runtime.health.InfinispanHealthCheck",
                buildTimeConfig.healthEnabled());
    }

    @BuildStep
    void registerServiceBinding(Capabilities capabilities, BuildProducer<ServiceProviderBuildItem> buildProducer) {
        if (capabilities.isPresent(Capability.KUBERNETES_SERVICE_BINDING)) {
            buildProducer.produce(
                    new ServiceProviderBuildItem(SERVICE_BINDING_INTERFACE_NAME,
                            InfinispanServiceBindingConverter.class.getName()));
        }
    }

    class RemoteCacheBean {
        Type type;
        String clientName;
        String cacheName;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RemoteCacheBean that = (RemoteCacheBean) o;
            return Objects.equals(type, that.type) && Objects.equals(clientName, that.clientName) && Objects.equals(
                    cacheName, that.cacheName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, clientName, cacheName);
        }
    }

    @BuildStep
    InjectionPointTransformerBuildItem transformInjectionPoints() {
        return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {
            @Override
            public void transform(TransformationContext ctx) {
                // If annotated with @Remote and no @InfinispanClientName is used, we need to add the default
                AnnotationInstance cacheNameAnnotation = Annotations.find(ctx.getQualifiers(), INFINISPAN_REMOTE_ANNOTATION);
                AnnotationInstance infinispanClientName = Annotations.find(ctx.getQualifiers(), INFINISPAN_CLIENT_ANNOTATION);
                if (cacheNameAnnotation != null && infinispanClientName == null) {
                    ctx.transform()
                            .add(INFINISPAN_CLIENT_ANNOTATION,
                                    AnnotationValue.createStringValue("value", DEFAULT_INFINISPAN_CLIENT_NAME))
                            .done();
                }
            }

            @Override
            public boolean appliesTo(Type requiredType) {
                return requiredType.name().equals(INFINISPAN_CACHE_CLIENT);
            }
        });
    }

    @Record(RUNTIME_INIT)
    @BuildStep
    void generateClientBeans(InfinispanRecorder recorder,
            BeanRegistrationPhaseBuildItem registrationPhase,
            BeanDiscoveryFinishedBuildItem finishedBuildItem,
            List<InfinispanClientNameBuildItem> infinispanClientNames,
            BeanDiscoveryFinishedBuildItem beans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {

        Set<String> clientNames = infinispanClientNames.stream().map(icn -> icn.getName()).collect(Collectors.toSet());

        Set<RemoteCacheBean> remoteCacheBeans = beans.getInjectionPoints().stream()
                .filter(ip -> ip.getRequiredQualifier(INFINISPAN_REMOTE_ANNOTATION) != null)
                .map(ip -> {
                    AnnotationInstance remoteCacheQualifier = ip.getRequiredQualifier(INFINISPAN_REMOTE_ANNOTATION);
                    AnnotationInstance clientNameQualifier = ip.getRequiredQualifier(INFINISPAN_CLIENT_ANNOTATION);

                    RemoteCacheBean remoteCacheBean = new RemoteCacheBean();
                    remoteCacheBean.type = ip.getType();
                    remoteCacheBean.cacheName = remoteCacheQualifier.value().asString();
                    remoteCacheBean.clientName = clientNameQualifier == null ? DEFAULT_INFINISPAN_CLIENT_NAME
                            : clientNameQualifier.value().asString();
                    return remoteCacheBean;
                })
                .collect(Collectors.toSet());

        if (!clientNames.contains(DEFAULT_INFINISPAN_CLIENT_NAME)) {
            boolean createDefaultCacheManager = finishedBuildItem.getInjectionPoints().stream()
                    .filter(i -> SUPPORTED_INJECTION_TYPE.contains(i.getRequiredType().name())
                            && i.getRequiredQualifier(INFINISPAN_CLIENT_ANNOTATION) == null)
                    .findAny()
                    .isPresent();
            if (createDefaultCacheManager) {
                clientNames.add(DEFAULT_INFINISPAN_CLIENT_NAME);
            }
        }

        // Produce default and/or named RemoteCacheManager and CounterManager beans
        for (String clientName : clientNames) {
            syntheticBeanBuildItemBuildProducer.produce(
                    configureAndCreateSyntheticBean(clientName, RemoteCacheManager.class,
                            recorder.infinispanClientSupplier(clientName)));
            syntheticBeanBuildItemBuildProducer.produce(
                    configureAndCreateSyntheticBean(clientName, CounterManager.class,
                            recorder.infinispanCounterManagerSupplier(clientName)));
        }

        // Produce RemoteCache beans
        for (RemoteCacheBean remoteCacheBean : remoteCacheBeans) {
            syntheticBeanBuildItemBuildProducer.produce(
                    configureAndCreateSyntheticBean(remoteCacheBean,
                            recorder.infinispanRemoteCacheClientSupplier(remoteCacheBean.clientName,
                                    remoteCacheBean.cacheName)));
        }
    }

    static <T> SyntheticBeanBuildItem configureAndCreateSyntheticBean(String name,
            Class<T> type,
            Supplier<T> supplier) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                .supplier(supplier)
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit();

        if (InfinispanClientUtil.isDefault(name)) {
            configurator.addQualifier(Default.class);
        } else {
            configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", name).done();
            configurator.addQualifier().annotation(INFINISPAN_CLIENT_ANNOTATION).addValue("value", name).done();
        }

        return configurator.done();
    }

    static <T> SyntheticBeanBuildItem configureAndCreateSyntheticBean(RemoteCacheBean remoteCacheBean, Supplier<T> supplier) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem.configure(RemoteCache.class)
                .types(remoteCacheBean.type)
                .scope(ApplicationScoped.class)
                .supplier(supplier)
                .unremovable()
                .setRuntimeInit();

        configurator.addQualifier().annotation(INFINISPAN_REMOTE_ANNOTATION).addValue("value", remoteCacheBean.cacheName)
                .done();

        configurator.addQualifier().annotation(INFINISPAN_CLIENT_ANNOTATION).addValue("value", remoteCacheBean.clientName)
                .done();
        return configurator.done();
    }

    @BuildStep
    @Record(value = RUNTIME_INIT, optional = true)
    List<InfinispanClientBuildItem> infinispanClients(InfinispanRecorder recorder,
            List<InfinispanClientNameBuildItem> infinispanClientNames,
            // make sure all beans have been initialized
            @SuppressWarnings("unused") BeanContainerBuildItem beanContainer) {
        List<InfinispanClientBuildItem> result = new ArrayList<>(infinispanClientNames.size());
        for (InfinispanClientNameBuildItem ic : infinispanClientNames) {
            String name = ic.getName();
            result.add(new InfinispanClientBuildItem(recorder.getClient(name), name));
        }
        return result;
    }

}
