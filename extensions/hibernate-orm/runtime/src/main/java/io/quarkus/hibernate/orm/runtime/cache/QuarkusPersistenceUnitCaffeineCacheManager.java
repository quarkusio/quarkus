package io.quarkus.hibernate.orm.runtime.cache;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;

/**
 * A JCache CacheManager that delegates to the Caffeine implementation,
 * but ensures all returned caches are configured with Quarkus settings.
 * <p>
 * The builder translates Quarkus cache configuration ({@code quarkus.hibernate-orm.cache.*})
 * into Caffeine {@link CaffeineConfiguration} instances and pre-creates caches at build time.
 */
public class QuarkusPersistenceUnitCaffeineCacheManager implements CacheManager {
    private final QuarkusPersistenceUnitCacheConfiguration configuration;
    private final CacheManager delegate;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    public QuarkusPersistenceUnitCaffeineCacheManager(String persistenceUnitName, boolean reactive,
            QuarkusPersistenceUnitCacheConfiguration configuration) {
        this.configuration = configuration;
        CachingProvider cachingProvider = Caching.getCachingProvider(
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        // Using a different cache manager per persistence unit because cache configuration (size, expiration) can be different,
        // forcing us to have separate caches.
        // TODO use the same cache manager / caches for ORM vs. Reactive? It wasn't the case with quarkus-local-cache.
        this.delegate = cachingProvider.getCacheManager(
                URI.create((reactive ? "quarkus/hibernate-reactive" : "quarkus/hibernate-orm")
                        + "/" + URLEncoder.encode(persistenceUnitName, StandardCharsets.UTF_8)),
                cachingProvider.getDefaultClassLoader());
        // Pre-create caches with explicit Quarkus-provided configuration.
        this.configuration.caches().keySet().forEach(this::getCache);
        // For other names, the first call to getCache() will yield a newly created cache with default config.
    }

    @Override
    public CachingProvider getCachingProvider() {
        return delegate.getCachingProvider();
    }

    @Override
    public URI getURI() {
        return delegate.getURI();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    @Override
    public Properties getProperties() {
        return delegate.getProperties();
    }

    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String cacheName, C configuration)
            throws IllegalArgumentException {
        // Should not happen as Hibernate ORM tries `getCache` first, and that should always return a non-null value.
        throw new UnsupportedOperationException("This should not be used by Hibernate ORM");
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        throw new UnsupportedOperationException("This should not be used by Hibernate ORM");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return (Cache<K, V>) caches.computeIfAbsent(cacheName, this::doCreateCache);
    }

    private <K, V> Cache<K, V> doCreateCache(String cacheName) {
        var quarkusConfig = this.configuration.caches().getOrDefault(cacheName,
                QuarkusPersistenceUnitCacheConfiguration.Cache.DEFAULT);
        CaffeineConfiguration<K, V> caffeineConfig = new CaffeineConfiguration<>();
        caffeineConfig.setMaximumSize(OptionalLong.of(quarkusConfig.maxSize()));
        caffeineConfig.setExpireAfterAccess(OptionalLong.of(quarkusConfig.maxIdle().toNanos()));
        return delegate.createCache(cacheName, caffeineConfig);
    }

    @Override
    public Iterable<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    @Override
    public void destroyCache(String cacheName) {
        throw new UnsupportedOperationException("This should not be used by Hibernate ORM");
    }

    @Override
    public void enableManagement(String cacheName, boolean enabled) {
        throw new UnsupportedOperationException("This should not be used by Hibernate ORM");
    }

    @Override
    public void enableStatistics(String cacheName, boolean enabled) {
        throw new UnsupportedOperationException("This should not be used by Hibernate ORM");
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return delegate.unwrap(clazz);
    }
}
