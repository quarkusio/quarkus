package io.quarkus.infinispan.client.runtime;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import io.quarkus.arc.Arc;

/**
 * Produces a configured remote cache manager instance
 */
@Singleton
public class InfinispanClientProducer {
    private static final Log log = LogFactory.getLog(InfinispanClientProducer.class);

    public static final StringConfiguration DEFAULT_CONFIG = new StringConfiguration(
            "<distributed-cache><encoding media-type=\"application/x-protostream\"/></distributed-cache>");
    public static final String PROTOBUF_FILE_PREFIX = "infinispan.client.hotrod.protofile.";
    public static final String PROTOBUF_INITIALIZERS = "infinispan.client.hotrod.proto-initializers";

    private final Map<String, RemoteCacheManager> remoteCacheManagers = new HashMap<>();

    @Inject
    private BeanManager beanManager;

    @Inject
    private Instance<InfinispanClientsRuntimeConfig> infinispanClientsRuntimeConfigHandle;

    @Inject
    private Instance<InfinispanClientsBuildTimeConfig> infinispanClientsBuildTimeConfigHandle;

    private void registerSchemaInServer(String infinispanConfigName,
            Map<String, Properties> properties,
            RemoteCacheManager cacheManager) {
        RemoteCache<String, String> protobufMetadataCache = null;
        Properties namedProperties = properties.get(infinispanConfigName);
        Set<SerializationContextInitializer> initializers = (Set) namedProperties.remove(PROTOBUF_INITIALIZERS);
        InfinispanClientRuntimeConfig runtimeConfig = this.infinispanClientsRuntimeConfigHandle.get()
                .getInfinispanClientRuntimeConfig(infinispanConfigName);
        if (initializers != null) {
            for (SerializationContextInitializer initializer : initializers) {
                if (protobufMetadataCache == null) {
                    protobufMetadataCache = cacheManager.getCache(
                            ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                }
                protobufMetadataCache.put(initializer.getProtoFileName(), initializer.getProtoFile());
            }
            runtimeConfig.backupCluster.entrySet().forEach(backup -> {
                if (backup.getValue().useSchemaRegistration.orElse(true)) {
                    cacheManager.switchToCluster(backup.getKey());
                    for (SerializationContextInitializer initializer : initializers) {
                        RemoteCache<String, String> backupProtobufMetadataCache = null;
                        if (backupProtobufMetadataCache == null) {
                            backupProtobufMetadataCache = cacheManager.getCache(
                                    ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                        }
                        backupProtobufMetadataCache.put(initializer.getProtoFileName(), initializer.getProtoFile());
                    }
                    cacheManager.switchToDefaultCluster();
                }
            });
        }

        for (Map.Entry<Object, Object> property : namedProperties.entrySet()) {
            Object key = property.getKey();
            if (key instanceof String) {
                String keyString = (String) key;
                if (keyString.startsWith(PROTOBUF_FILE_PREFIX)) {
                    String fileName = keyString.substring(PROTOBUF_FILE_PREFIX.length());
                    String fileContents = (String) property.getValue();
                    if (protobufMetadataCache == null) {
                        protobufMetadataCache = cacheManager.getCache(
                                ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                    }
                    protobufMetadataCache.put(fileName, fileContents);
                }
            }
        }

        runtimeConfig.backupCluster.entrySet().forEach(backupConfigEntry -> {
            if (backupConfigEntry.getValue().useSchemaRegistration.orElse(true)) {
                cacheManager.switchToCluster(backupConfigEntry.getKey());
                RemoteCache<String, String> backupProtobufMetadataCache = null;
                for (Map.Entry<Object, Object> property : namedProperties.entrySet()) {
                    Object key = property.getKey();
                    if (key instanceof String) {
                        String keyString = (String) key;
                        if (keyString.startsWith(PROTOBUF_FILE_PREFIX)) {
                            String fileName = keyString.substring(PROTOBUF_FILE_PREFIX.length());
                            String fileContents = (String) property.getValue();
                            if (backupProtobufMetadataCache == null) {
                                backupProtobufMetadataCache = cacheManager.getCache(
                                        ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
                            }
                            backupProtobufMetadataCache.put(fileName, fileContents);
                        }
                    }
                }
                cacheManager.switchToDefaultCluster();
            }
        });
    }

    private void initialize(String infinispanConfigName, Map<String, Properties> properties) {
        log.debug("Initializing default RemoteCacheManager");
        if (properties.isEmpty()) {
            // We already loaded and it wasn't present - so don't initialize the cache manager
            return;
        }

        ConfigurationBuilder conf = builderFromProperties(infinispanConfigName, properties);
        if (conf.servers().isEmpty()) {
            return;
        }
        // Build de cache manager if the server list is present
        RemoteCacheManager cacheManager = new RemoteCacheManager(conf.build());
        remoteCacheManagers.put(infinispanConfigName, cacheManager);

        InfinispanClientsRuntimeConfig infinispanClientsRuntimeConfig = this.infinispanClientsRuntimeConfigHandle.get();

        if (infinispanClientsRuntimeConfig.useSchemaRegistration.orElse(Boolean.TRUE)) {
            registerSchemaInServer(infinispanConfigName, properties, cacheManager);
        }
    }

    /**
     * Configures the client using the client name
     *
     * @return the configuration builder based on the provided properties
     * @throws RuntimeException if the cache configuration file is not present in the resources folder
     */
    private ConfigurationBuilder builderFromProperties(String infinispanClientName, Map<String, Properties> propertiesMap) {
        // If you are changing this method, you will most likely have to change replaceProperties as well
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Properties properties = propertiesMap.get(infinispanClientName);
        // remove from properties
        Object marshallerInstance = properties.remove(ConfigurationProperties.MARSHALLER);
        if (marshallerInstance != null) {
            if (marshallerInstance instanceof ProtoStreamMarshaller) {
                handleProtoStreamMarshaller((ProtoStreamMarshaller) marshallerInstance, properties, beanManager);
            }
            // add to the builder directly
            builder.marshaller((Marshaller) marshallerInstance);
        }

        InfinispanClientRuntimeConfig infinispanClientRuntimeConfig = infinispanClientsRuntimeConfigHandle.get()
                .getInfinispanClientRuntimeConfig(infinispanClientName);

        InfinispanClientBuildTimeConfig infinispanClientBuildTimeConfig = infinispanClientsBuildTimeConfigHandle.get()
                .getInfinispanClientBuildTimeConfig(infinispanClientName);

        // client name not found
        if (infinispanClientRuntimeConfig == null) {
            return builder;
        }

        if (infinispanClientRuntimeConfig.uri.isPresent()) {
            properties.put(ConfigurationProperties.URI, infinispanClientRuntimeConfig.uri.get());
        } else {
            if (infinispanClientRuntimeConfig.serverList.isPresent()) {
                log.warn(
                        "Use 'quarkus.infinispan-client.hosts' instead of the deprecated 'quarkus.infinispan-client.server-list'");
                properties.put(ConfigurationProperties.SERVER_LIST, infinispanClientRuntimeConfig.serverList.get());
            }

            if (infinispanClientRuntimeConfig.hosts.isPresent()) {
                properties.put(ConfigurationProperties.SERVER_LIST, infinispanClientRuntimeConfig.hosts.get());
            }

            if (infinispanClientRuntimeConfig.authUsername.isPresent()) {
                log.warn(
                        "Use 'quarkus.infinispan-client.username' instead of the deprecated 'quarkus.infinispan-client.auth-username'");
                properties.put(ConfigurationProperties.AUTH_USERNAME, infinispanClientRuntimeConfig.authUsername.get());
            }

            if (infinispanClientRuntimeConfig.username.isPresent()) {
                properties.put(ConfigurationProperties.AUTH_USERNAME, infinispanClientRuntimeConfig.username.get());
            }

            if (infinispanClientRuntimeConfig.authPassword.isPresent()) {
                log.warn(
                        "Use 'quarkus.infinispan-client.password' instead of the deprecated 'quarkus.infinispan-client.auth-password'");
                properties.put(ConfigurationProperties.AUTH_PASSWORD, infinispanClientRuntimeConfig.authPassword.get());
            }

            if (infinispanClientRuntimeConfig.password.isPresent()) {
                properties.put(ConfigurationProperties.AUTH_PASSWORD, infinispanClientRuntimeConfig.password.get());
            }
        }

        if (infinispanClientRuntimeConfig.clientIntelligence.isPresent()) {
            properties.put(ConfigurationProperties.CLIENT_INTELLIGENCE, infinispanClientRuntimeConfig.clientIntelligence.get());
        }

        if (infinispanClientRuntimeConfig.useAuth.isPresent()) {
            properties.put(ConfigurationProperties.USE_AUTH, infinispanClientRuntimeConfig.useAuth.get());
        }

        if (infinispanClientRuntimeConfig.authRealm.isPresent()) {
            properties.put(ConfigurationProperties.AUTH_REALM, infinispanClientRuntimeConfig.authRealm.get());
        }
        if (infinispanClientRuntimeConfig.authServerName.isPresent()) {
            properties.put(ConfigurationProperties.AUTH_SERVER_NAME, infinispanClientRuntimeConfig.authServerName.get());
        }

        if (infinispanClientRuntimeConfig.saslMechanism.isPresent()) {
            properties.put(ConfigurationProperties.SASL_MECHANISM, infinispanClientRuntimeConfig.saslMechanism.get());
        }

        if (infinispanClientRuntimeConfig.trustStore.isPresent()) {
            properties.put(ConfigurationProperties.TRUST_STORE_FILE_NAME, infinispanClientRuntimeConfig.trustStore.get());
        }
        if (infinispanClientRuntimeConfig.trustStorePassword.isPresent()) {
            properties.put(ConfigurationProperties.TRUST_STORE_PASSWORD,
                    infinispanClientRuntimeConfig.trustStorePassword.get());
        }
        if (infinispanClientRuntimeConfig.trustStoreType.isPresent()) {
            properties.put(ConfigurationProperties.TRUST_STORE_TYPE, infinispanClientRuntimeConfig.trustStoreType.get());
        }

        if (infinispanClientRuntimeConfig.sslProvider.isPresent()) {
            properties.put(ConfigurationProperties.SSL_PROVIDER, infinispanClientRuntimeConfig.sslProvider.get());
        }

        if (infinispanClientRuntimeConfig.sslProtocol.isPresent()) {
            properties.put(ConfigurationProperties.SSL_PROTOCOL, infinispanClientRuntimeConfig.sslProtocol.get());
        }

        if (infinispanClientRuntimeConfig.sslCiphers.isPresent()) {
            properties.put(ConfigurationProperties.SSL_CIPHERS,
                    infinispanClientRuntimeConfig.sslCiphers.get().stream().collect(Collectors.joining(" ")));
        }

        if (infinispanClientRuntimeConfig.sslHostNameValidation.isPresent()) {
            properties.put(ConfigurationProperties.SSL_HOSTNAME_VALIDATION,
                    infinispanClientRuntimeConfig.sslHostNameValidation.get());
        }

        if (infinispanClientRuntimeConfig.sniHostName.isPresent()) {
            properties.put(ConfigurationProperties.SNI_HOST_NAME, infinispanClientRuntimeConfig.sniHostName.get());
        }

        builder.withProperties(properties);

        if (infinispanClientRuntimeConfig.tracingPropagationEnabled.isPresent()) {
            if (!infinispanClientRuntimeConfig.tracingPropagationEnabled.get()) {
                builder.disableTracingPropagation();
            }
        }

        if (infinispanClientBuildTimeConfig != null) {
            for (Map.Entry<String, InfinispanClientBuildTimeConfig.RemoteCacheConfig> buildCacheConfig : infinispanClientBuildTimeConfig.cache
                    .entrySet()) {
                String cacheName = buildCacheConfig.getKey();
                // Do this if the cache config is only present in the build time configuration
                if (!infinispanClientRuntimeConfig.cache.containsKey(cacheName)) {
                    InfinispanClientBuildTimeConfig.RemoteCacheConfig buildCacheConfigValue = buildCacheConfig.getValue();
                    if (buildCacheConfig.getValue().configurationResource.isPresent()) {
                        URL configFile = Thread.currentThread().getContextClassLoader()
                                .getResource(buildCacheConfigValue.configurationResource.get());
                        configureRemoteCacheConfigurationURI(builder, cacheName, configFile);
                    }
                }
            }
        }

        for (Map.Entry<String, InfinispanClientRuntimeConfig.RemoteCacheConfig> cacheConfig : infinispanClientRuntimeConfig.cache
                .entrySet()) {
            String cacheName = cacheConfig.getKey();
            InfinispanClientRuntimeConfig.RemoteCacheConfig runtimeCacheConfig = cacheConfig.getValue();
            URL configFile = null;
            // Check if the build time resource file configuration exists
            if (infinispanClientBuildTimeConfig != null) {
                InfinispanClientBuildTimeConfig.RemoteCacheConfig buildtimeCacheConfig = infinispanClientBuildTimeConfig.cache
                        .get(
                                cacheName);
                if (buildtimeCacheConfig != null && buildtimeCacheConfig.configurationResource.isPresent()) {
                    configFile = Thread.currentThread().getContextClassLoader()
                            .getResource(buildtimeCacheConfig.configurationResource.get());
                }
            }

            // Override build time resource if configuration-uri runtime resource is provided
            if (runtimeCacheConfig.configurationUri.isPresent()) {
                configFile = Thread.currentThread().getContextClassLoader()
                        .getResource(runtimeCacheConfig.configurationUri.get());
            }

            if (configFile != null) {
                configureRemoteCacheConfigurationURI(builder, cacheName, configFile);
            } else {
                // Inline configuration
                if (runtimeCacheConfig.configuration.isPresent()) {
                    builder.remoteCache(cacheName).configuration(runtimeCacheConfig.configuration.get());
                }
            }

            // Configures near caching
            if (runtimeCacheConfig.nearCacheMaxEntries.isPresent()) {
                builder.remoteCache(cacheName).nearCacheMaxEntries(runtimeCacheConfig.nearCacheMaxEntries.get());
            }
            if (runtimeCacheConfig.nearCacheMode.isPresent()) {
                builder.remoteCache(cacheName).nearCacheMode(runtimeCacheConfig.nearCacheMode.get());
            }
            if (runtimeCacheConfig.nearCacheUseBloomFilter.isPresent()) {
                builder.remoteCache(cacheName).nearCacheUseBloomFilter(runtimeCacheConfig.nearCacheUseBloomFilter.get());
            }
        }

        for (Map.Entry<String, InfinispanClientRuntimeConfig.BackupClusterConfig> backupCluster : infinispanClientRuntimeConfig.backupCluster
                .entrySet()) {
            InfinispanClientRuntimeConfig.BackupClusterConfig backupClusterConfig = backupCluster.getValue();
            ClusterConfigurationBuilder clusterConfigurationBuilder = builder.addCluster(backupCluster.getKey());
            clusterConfigurationBuilder.addClusterNodes(backupClusterConfig.hosts);
            if (backupClusterConfig.clientIntelligence.isPresent()) {
                clusterConfigurationBuilder.clusterClientIntelligence(
                        ClientIntelligence.valueOf(backupClusterConfig.clientIntelligence.get()));
            }
        }

        return builder;
    }

    private void configureRemoteCacheConfigurationURI(ConfigurationBuilder builder, String cacheName, URL configFile) {
        try {
            builder.remoteCache(cacheName).configurationURI(configFile.toURI());
        } catch (Exception e) {
            log.errorf("Provided configuration-resource or configuration-uri can't be loaded. %s",
                    configFile.getPath());
            throw new IllegalStateException(e);
        }
    }

    private static void handleProtoStreamMarshaller(ProtoStreamMarshaller marshaller, Properties properties,
            BeanManager beanManager) {
        SerializationContext serializationContext = marshaller.getSerializationContext();

        Set<SerializationContextInitializer> initializers = (Set) properties
                .get(PROTOBUF_INITIALIZERS);
        if (initializers != null) {
            for (SerializationContextInitializer initializer : initializers) {
                initializer.registerSchema(serializationContext);
                initializer.registerMarshallers(serializationContext);
            }
        }

        FileDescriptorSource fileDescriptorSource = null;
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            Object key = property.getKey();
            if (key instanceof String) {
                String keyString = (String) key;
                if (keyString.startsWith(PROTOBUF_FILE_PREFIX)) {
                    String fileName = keyString.substring(PROTOBUF_FILE_PREFIX.length());
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
            // Register all the fds so they can be queried
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
        remoteCacheManagers.values().forEach(rcm -> rcm.stop());
    }

    public <K, V> RemoteCache<K, V> getRemoteCache(String clientName, String cacheName) {
        RemoteCacheManager cacheManager;
        if (InfinispanClientUtil.isDefault(clientName)) {
            cacheManager = Arc.container().instance(RemoteCacheManager.class, Default.Literal.INSTANCE).get();
        } else {
            cacheManager = Arc.container().instance(RemoteCacheManager.class, NamedLiteral.of(clientName))
                    .get();
        }

        if (cacheManager != null && cacheName != null && !cacheName.isEmpty()) {
            RemoteCache<K, V> cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Attempt to create cache using minimal default config");
                return cacheManager.administration()
                        .getOrCreateCache(cacheName, DEFAULT_CONFIG);
            }
            return cache;
        }

        if (cacheManager != null) {
            RemoteCache<K, V> cache = cacheManager.getCache();
            if (cache == null) {
                log.warn("Attempt to create cache using minimal default config");
                return cacheManager.administration()
                        .getOrCreateCache(cacheName, DEFAULT_CONFIG);
            }
            return cache;
        }

        log.error("Unable to produce RemoteCache. RemoteCacheManager is null. Client: " + cacheName);
        throw new IllegalStateException(
                "Unable to produce RemoteCache. RemoteCacheManager is null. Client: " + cacheName);
    }

    Map<String, Properties> properties;

    public void setProperties(Map<String, Properties> properties) {
        this.properties = properties;
    }

    public RemoteCacheManager getNamedRemoteCacheManager(String clientName) {
        if (!remoteCacheManagers.containsKey(clientName)) {
            initialize(clientName, properties);
        }
        return remoteCacheManagers.get(clientName);
    }

    public CounterManager getNamedCounterManager(String clientName) {
        RemoteCacheManager cacheManager = getNamedRemoteCacheManager(clientName);
        return RemoteCounterManagerFactory.asCounterManager(cacheManager);
    }

}
