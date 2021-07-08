package io.quarkus.infinispan.client.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.EnumMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.RawProtobufMarshaller;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.ApplicationArchive;
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
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.infinispan.client.runtime.InfinispanClientBuildTimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanClientProducer;
import io.quarkus.infinispan.client.runtime.InfinispanRecorder;

class InfinispanClientProcessor {
    private static final Log log = LogFactory.getLog(InfinispanClientProcessor.class);

    private static final String META_INF = "META-INF";
    private static final String HOTROD_CLIENT_PROPERTIES = META_INF + File.separator + "hotrod-client.properties";
    private static final String PROTO_EXTENSION = ".proto";

    /**
     * The Infinispan client build time configuration.
     */
    InfinispanClientBuildTimeConfig infinispanClient;

    @BuildStep
    InfinispanPropertiesBuildItem setup(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeployment,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<NativeImageConfigBuildItem> nativeImageConfig,
            CombinedIndexBuildItem applicationIndexBuildItem) throws ClassNotFoundException, IOException {

        feature.produce(new FeatureBuildItem(Feature.INFINISPAN_CLIENT));
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(InfinispanClientProducer.class));
        systemProperties.produce(new SystemPropertyBuildItem("io.netty.noUnsafe", "true"));
        hotDeployment.produce(new HotDeploymentWatchedFileBuildItem(HOTROD_CLIENT_PROPERTIES));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.INFINISPAN_CLIENT));

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(HOTROD_CLIENT_PROPERTIES);
        Properties properties;
        if (stream == null) {
            properties = new Properties();
            if (log.isTraceEnabled()) {
                log.trace("There was no hotrod-client.properties file found - using defaults");
            }
        } else {
            try {
                properties = loadFromStream(stream);
                if (log.isDebugEnabled()) {
                    log.debugf("Found HotRod properties of %s", properties);
                }
            } finally {
                Util.close(stream);
            }

            // We use caffeine for bounded near cache - so register that reflection if we have a bounded near cache
            if (properties.containsKey(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES)) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.github.benmanes.caffeine.cache.SSMS"));
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.github.benmanes.caffeine.cache.PSMS"));
            }
        }

        InfinispanClientProducer.replaceProperties(properties);

        IndexView index = applicationIndexBuildItem.getIndex();

        // This is always non null
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
                            System.out.println("  " + path.toAbsolutePath());
                            byte[] bytes = Files.readAllBytes(path);
                            // This uses the default file encoding - should we enforce UTF-8?
                            properties.put(InfinispanClientProducer.PROTOBUF_FILE_PREFIX + path.getFileName().toString(),
                                    new String(bytes, StandardCharsets.UTF_8));
                        }
                    }
                }
            }

            InfinispanClientProducer.handleProtoStreamRequirements(properties);
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

        // Add any user project listeners to allow reflection in native code
        Collection<AnnotationInstance> listenerInstances = index.getAnnotations(
                DotName.createSimple(ClientListener.class.getName()));
        for (AnnotationInstance instance : listenerInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, target.asClass().name().toString()));
            }
        }

        // This is required for netty to work properly
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "io.netty.channel.socket.nio.NioSocketChannel"));
        // We use reflection to have continuous queries work
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.infinispan.client.hotrod.event.impl.ContinuousQueryImpl$ClientEntryListener"));
        // We use reflection to allow for near cache invalidations
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "org.infinispan.client.hotrod.near.NearCacheService$InvalidatedNearCacheListener"));
        // This is required when a cache is clustered to tell us topology
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "org.infinispan.client.hotrod.impl.consistenthash.SegmentConsistentHash"));

        return new InfinispanPropertiesBuildItem(properties);
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

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    BeanContainerListenerBuildItem build(InfinispanRecorder recorder, InfinispanPropertiesBuildItem builderBuildItem) {
        Properties properties = builderBuildItem.getProperties();
        InfinispanClientBuildTimeConfig conf = infinispanClient;
        if (log.isDebugEnabled()) {
            log.debugf("Applying micro profile configuration: %s", conf);
        }
        int maxEntries = conf.nearCacheMaxEntries;
        // Only write the entries if it is a valid number and it isn't already configured
        if (maxEntries > 0 && !properties.containsKey(ConfigurationProperties.NEAR_CACHE_MODE)) {
            // This is already empty so no need for putIfAbsent
            properties.put(ConfigurationProperties.NEAR_CACHE_MODE, NearCacheMode.INVALIDATED.toString());
            properties.putIfAbsent(ConfigurationProperties.NEAR_CACHE_MAX_ENTRIES, maxEntries);
        }

        return new BeanContainerListenerBuildItem(recorder.configureInfinispan(properties));
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return UnremovableBeanBuildItem.beanTypes(BaseMarshaller.class, EnumMarshaller.class, MessageMarshaller.class,
                RawProtobufMarshaller.class, FileDescriptorSource.class);
    }
}
