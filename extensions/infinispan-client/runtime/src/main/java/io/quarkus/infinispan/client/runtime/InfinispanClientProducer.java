package io.quarkus.infinispan.client.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.query.remote.client.impl.MarshallerRegistration;

/**
 * Produces a configured remote cache manager instance
 */
@ApplicationScoped
public class InfinispanClientProducer {
    private static final Log log = LogFactory.getLog(InfinispanClientProducer.class);

    public static final String PROTOBUF_FILE_PREFIX = "infinispan.client.hotrod.protofile.";
    public static final String PROTOBUF_INITIALIZERS = "infinispan.client.hotrod.proto-initializers";

    @Inject
    private BeanManager beanManager;

    private Properties properties;
    private RemoteCacheManager cacheManager;
    private InfinispanClientRuntimeConfig infinispanClientRuntimeConfig;

    public void setRuntimeConfig(InfinispanClientRuntimeConfig infinispanClientConfigRuntime) {
        this.infinispanClientRuntimeConfig = infinispanClientConfigRuntime;
    }

    private void initialize() {
        log.debug("Initializing CacheManager");
        Configuration conf;
        if (properties == null) {
            // We already loaded and it wasn't present - so use an empty config
            conf = new ConfigurationBuilder().build();
        } else {
            conf = builderFromProperties(properties).build();
        }
        cacheManager = new RemoteCacheManager(conf);

        // TODO: do we want to automatically register all the proto file definitions?
        RemoteCache<String, String> protobufMetadataCache = null;

        Set<SerializationContextInitializer> initializers = (Set) properties.remove(PROTOBUF_INITIALIZERS);
        if (initializers != null) {
            for (SerializationContextInitializer initializer : initializers) {
                if (protobufMetadataCache == null) {
                    protobufMetadataCache = cacheManager.getCache(
                            ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                }
                protobufMetadataCache.put(initializer.getProtoFileName(), initializer.getProtoFile());
            }
        }

        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            Object key = property.getKey();
            if (key instanceof String) {
                String keyString = (String) key;
                if (keyString.startsWith(InfinispanClientProducer.PROTOBUF_FILE_PREFIX)) {
                    String fileName = keyString.substring(InfinispanClientProducer.PROTOBUF_FILE_PREFIX.length());
                    String fileContents = (String) property.getValue();
                    if (protobufMetadataCache == null) {
                        protobufMetadataCache = cacheManager.getCache(
                                ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                    }
                    protobufMetadataCache.put(fileName, fileContents);
                }
            }
        }
    }

    /**
     * This method is designed to be called during static initialization time. This is so we have access to the
     * classes, and thus we can use reflection to find and instantiate any instances we may need
     *
     * @param properties properties file read from hot rod
     * @throws ClassNotFoundException if a class is not actually found that should be present
     */
    public static void replaceProperties(Properties properties) throws ClassNotFoundException {
        // If you are changing this method, you will most likely have to change builderFromProperties as well
        String marshallerClassName = (String) properties.get(ConfigurationProperties.MARSHALLER);
        if (marshallerClassName != null) {
            Class<?> marshallerClass = Class.forName(marshallerClassName);
            properties.put(ConfigurationProperties.MARSHALLER, Util.getInstance(marshallerClass));
        } else {
            // Default to proto stream marshaller if one is not provided
            properties.put(ConfigurationProperties.MARSHALLER, new ProtoStreamMarshaller());
        }
    }

    /**
     * Sets up additional properties for use when proto stream marshaller is in use
     *
     * @param properties the properties to be updated for querying
     */
    public static void handleProtoStreamRequirements(Properties properties) {
        // We only apply this if we are substrate in build time to apply to the properties
        // Note that the other half is done in QuerySubstitutions.SubstituteMarshallerRegistration class
        // Note that the registration of these files are done twice in normal VM mode
        // (once during init and once at runtime)
        properties.put(InfinispanClientProducer.PROTOBUF_FILE_PREFIX + MarshallerRegistration.QUERY_PROTO_RES,
                getContents(MarshallerRegistration.QUERY_PROTO_RES));
        properties.put(InfinispanClientProducer.PROTOBUF_FILE_PREFIX + MarshallerRegistration.MESSAGE_PROTO_RES,
                getContents(MarshallerRegistration.MESSAGE_PROTO_RES));
    }

    /**
     * Reads all the contents of the file as a single string using default charset
     *
     * @param fileName file on class path to read contents of
     * @return string containing the contents of the file
     */
    private static String getContents(String fileName) {
        InputStream stream = InfinispanClientProducer.class.getResourceAsStream(fileName);
        try (Scanner scanner = new Scanner(stream, "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    /**
     * The mirror side of {@link #replaceProperties(Properties)} so that we can take out any objects that were
     * instantiated during static init time and inject them properly
     *
     * @param properties the properties that was static constructed
     * @return the configuration builder based on the provided properties
     */
    private ConfigurationBuilder builderFromProperties(Properties properties) {
        // If you are changing this method, you will most likely have to change replaceProperties as well
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Object marshallerInstance = properties.remove(ConfigurationProperties.MARSHALLER);
        if (marshallerInstance != null) {
            if (marshallerInstance instanceof ProtoStreamMarshaller) {
                handleProtoStreamMarshaller((ProtoStreamMarshaller) marshallerInstance, properties, beanManager);
            }
            builder.marshaller((Marshaller) marshallerInstance);
        }

        // Override serverList property value at runtime if such configuration exists
        if (infinispanClientRuntimeConfig != null) {
            Optional<String> runtimeServerList = infinispanClientRuntimeConfig.serverList;
            if (runtimeServerList.isPresent()) {
                properties.put(ConfigurationProperties.SERVER_LIST, runtimeServerList.get());
            }
        }

        builder.withProperties(properties);

        return builder;
    }

    private static void handleProtoStreamMarshaller(ProtoStreamMarshaller marshaller, Properties properties,
            BeanManager beanManager) {
        SerializationContext serializationContext = marshaller.getSerializationContext();

        Set<SerializationContextInitializer> initializers = (Set) properties
                .get(InfinispanClientProducer.PROTOBUF_INITIALIZERS);
        if (initializers != null) {
            initializers.forEach(sci -> {
                try {
                    sci.registerSchema(serializationContext);
                } catch (IOException e) {
                    throw new CacheConfigurationException(e);
                }
                sci.registerMarshallers(serializationContext);
            });
        }

        FileDescriptorSource fileDescriptorSource = null;
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            Object key = property.getKey();
            if (key instanceof String) {
                String keyString = (String) key;
                if (keyString.startsWith(InfinispanClientProducer.PROTOBUF_FILE_PREFIX)) {
                    String fileName = keyString.substring(InfinispanClientProducer.PROTOBUF_FILE_PREFIX.length());
                    String fileContents = (String) property.getValue();
                    if (fileDescriptorSource == null) {
                        fileDescriptorSource = new FileDescriptorSource();
                    }
                    fileDescriptorSource.addProtoFile(fileName, fileContents);
                }
            }
        }

        if (fileDescriptorSource != null) {
            serializationContext.registerProtoFiles(fileDescriptorSource);
        }

        Set<Bean<FileDescriptorSource>> protoFileBeans = (Set) beanManager.getBeans(FileDescriptorSource.class);
        for (Bean<FileDescriptorSource> bean : protoFileBeans) {
            CreationalContext<FileDescriptorSource> ctx = beanManager.createCreationalContext(bean);
            FileDescriptorSource fds = (FileDescriptorSource) beanManager.getReference(bean, FileDescriptorSource.class,
                    ctx);
            serializationContext.registerProtoFiles(fds);
            // Register all of the fds so they can be queried
            for (Map.Entry<String, char[]> fdEntry : fds.getFileDescriptors().entrySet()) {
                properties.put(PROTOBUF_FILE_PREFIX + fdEntry.getKey(), new String(fdEntry.getValue()));
            }
        }

        Set<Bean<BaseMarshaller>> beans = (Set) beanManager.getBeans(BaseMarshaller.class);
        for (Bean<BaseMarshaller> bean : beans) {
            CreationalContext<BaseMarshaller> ctx = beanManager.createCreationalContext(bean);
            BaseMarshaller messageMarshaller = (BaseMarshaller) beanManager.getReference(bean, BaseMarshaller.class,
                    ctx);
            serializationContext.registerMarshaller(messageMarshaller);
        }
    }

    @PreDestroy
    public void destroy() {
        if (cacheManager != null) {
            cacheManager.stop();
        }
    }

    @Remote
    @Produces
    public <K, V> RemoteCache<K, V> getRemoteCache(InjectionPoint injectionPoint, RemoteCacheManager cacheManager) {
        Set<Annotation> annotationSet = injectionPoint.getQualifiers();
        final Remote remote = getRemoteAnnotation(annotationSet);

        if (remote != null && !remote.value().isEmpty()) {
            return cacheManager.getCache(remote.value());
        }
        return cacheManager.getCache();
    }

    @Produces
    public CounterManager counterManager() {
        return RemoteCounterManagerFactory.asCounterManager(remoteCacheManager());
    }

    @Produces
    public synchronized RemoteCacheManager remoteCacheManager() {
        if (cacheManager != null) {
            return cacheManager;
        }
        initialize();
        return cacheManager;
    }

    void configure(Properties properties) {
        this.properties = properties;
    }

    /**
     * Retrieves the {@link Remote} annotation instance from the set
     *
     * @param annotationSet the annotation set.
     * @return the {@link Remote} annotation instance or {@code null} if not found.
     */
    private Remote getRemoteAnnotation(Set<Annotation> annotationSet) {
        for (Annotation annotation : annotationSet) {
            if (annotation instanceof Remote) {
                return (Remote) annotation;
            }
        }
        return null;
    }
}
