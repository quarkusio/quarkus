package io.quarkus.hibernate.orm.runtime.service.internalcache;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.internal.util.cache.InternalCache;
import org.hibernate.internal.util.cache.InternalCacheFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Override of {@link org.hibernate.internal.util.cache.InternalCacheFactoryInitiator}:
 * this switches the internal cache implementation (currently used for some stats and, crucially, for the
 * {@link org.hibernate.query.spi.QueryInterpretationCache}
 * to use Caffeine rather than the legacy implementation Hibernate ORM normally uses, which is based on the excellent LIRS
 * algorithm but which we
 * plan to deprecate in favour of modern caching libraries.
 * See also <a href="https://en.wikipedia.org/wiki/LIRS_caching_algorithm">LIRS</a> and
 * <a href="https://github.com/ben-manes/caffeine/wiki/Efficiency">Caffeine, efficiency</a>.
 */
public final class QuarkusInternalCacheFactoryInitiator implements StandardServiceInitiator<InternalCacheFactory> {

    public static final QuarkusInternalCacheFactoryInitiator INSTANCE = new QuarkusInternalCacheFactoryInitiator();

    private QuarkusInternalCacheFactoryInitiator() {
    }

    @Override
    public InternalCacheFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
        return new QuarkusInternalCacheFactory();
    }

    @Override
    public Class<InternalCacheFactory> getServiceInitiated() {
        return InternalCacheFactory.class;
    }

    private static class QuarkusInternalCacheFactory implements InternalCacheFactory {
        @Override
        public <K, V> InternalCache<K, V> createInternalCache(int intendedApproximateSize) {
            final Cache<K, V> caffeineCache = Caffeine.newBuilder()
                    .maximumSize(intendedApproximateSize)
                    .build();
            return new QuarkusInternalCache<>(caffeineCache);
        }
    }
}
