package org.infinispan.protean.runtime;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
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
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * Produces a configured remote cache manager instance
 */
@ApplicationScoped
public class InfinispanClientProducer {
   private static final Log log = LogFactory.getLog(InfinispanClientProducer.class);

   public static final String PROTOBUF_MARSHALLER_CLASS_NAME = "org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller";
   public static final String PROTOBUF_FILE_PREFIX = "infinispan.client.hotrod.protofile.";

    private Properties properties;
    private RemoteCacheManager cacheManager;
    @Inject
    private BeanManager beanManager;

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
    * @param properties properties file read from hot rod
    * @throws ClassNotFoundException if a class is not actually found that should be present
    */
    public static void replaceProperties(Properties properties) throws ClassNotFoundException {
        // If you are changing this method, you will most likely have to change builderFromProperties as well
        String marshallerClassName = (String) properties.get(ConfigurationProperties.MARSHALLER);
        if (marshallerClassName != null) {
            Class<?> marshallerClass = Class.forName(marshallerClassName);
            properties.put(ConfigurationProperties.MARSHALLER, Util.getInstance(marshallerClass));
        }
    }

   /**
    * Sets up additional properties for use when querying is available. Note this method should only be invoked if
    * protobuf is available (ie. {@link #isProtoBufAvailable(Object)}).
    * @param properties the properties to be updated for querying
    */
    public static void handleQueryRequirements(Properties properties) {
       // We only apply this if we are substrate in build time to apply to the properties
       // Note that the other half is done in QuerySubstitutions.SubstituteMarshallerRegistration class
       // Note that the registration of these files are done twice in normal VM mode
       // (once during init and once at runtime)
       HandleProtostreamMarshaller.handleQueryRequirements(properties);
    }

   /**
    * The mirror side of {@link #replaceProperties(Properties)} so that we can take out any objects that were
    * instantiated during static init time and inject them properly
    * @param properties the properties that was static constructed
    * @return the configuration builder based on the provided properties
    */
    private ConfigurationBuilder builderFromProperties(Properties properties) {
       // If you are changing this method, you will most likely have to change replaceProperties as well
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Object marshallerInstance = properties.remove(ConfigurationProperties.MARSHALLER);
        if (marshallerInstance != null) {
            injectProtoMarshallers(marshallerInstance, properties);
            builder.marshaller((Marshaller) marshallerInstance);
        }
        builder.withProperties(properties);
        return builder;
    }

    // NOTE: that this method is removed during substitution in graal if protostream is not in the class path
    private void injectProtoMarshallers(Object marshallerInstance, Properties properties) {
       if (isProtoBufAvailable(marshallerInstance)) {
          HandleProtostreamMarshaller.handlePossibleMarshaller(marshallerInstance, properties, beanManager);
       }
    }

    public static boolean isProtoBufAvailable(Object marshallerInstance) {
       // proto stream is optional and we can't do Class.forName at Runtime and we don't want to attempt to load
       // it in this class so we check the name and if present invoke another class that handles it
       return marshallerInstance != null &&
             marshallerInstance.getClass().getName().equals(PROTOBUF_MARSHALLER_CLASS_NAME);
    }

    @PreDestroy
    public void destroy() {
        if (cacheManager != null) {
            cacheManager.stop();
        }
    }

    @Remote
    @Produces
    public <K, V> RemoteCache<K, V> getRemoteCache(InjectionPoint injectionPoint) {
        final RemoteCacheManager cacheManager = remoteCacheManager();
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
