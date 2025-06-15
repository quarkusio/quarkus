package io.quarkus.cache.runtime;

import static io.quarkus.cache.runtime.CacheBuildConfig.CAFFEINE_CACHE_TYPE;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;

import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheManagerInfo;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheManagerBuilder;
import io.quarkus.cache.runtime.noop.NoOpCacheManagerBuilder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CacheManagerRecorder {

    private final CacheBuildConfig cacheBuildConfig;
    private final RuntimeValue<CacheConfig> cacheConfigRV;

    public CacheManagerRecorder(CacheBuildConfig cacheBuildConfig, RuntimeValue<CacheConfig> cacheConfigRV) {
        this.cacheBuildConfig = cacheBuildConfig;
        this.cacheConfigRV = cacheConfigRV;
    }

    public Supplier<CacheManager> resolveCacheInfo(Collection<CacheManagerInfo> infos, Set<String> cacheNames,
            boolean micrometerMetricsEnabled) {
        CacheConfig cacheConfig = cacheConfigRV.getValue();
        CacheManagerInfo.Context context = new CacheManagerInfo.Context() {
            @Override
            public boolean cacheEnabled() {
                return cacheConfig.enabled();
            }

            @Override
            public Metrics metrics() {
                return micrometerMetricsEnabled ? Metrics.MICROMETER : Metrics.NONE;
            }

            @Override
            public String cacheType() {
                return cacheBuildConfig.type();
            }

            @Override
            public Set<String> cacheNames() {
                return cacheNames;
            }
        };
        for (CacheManagerInfo info : infos) {
            if (info.supports(context)) {
                return info.get(context);
            }
        }
        throw new DeploymentException("Unknown cache type: " + context.cacheType());
    }

    public CacheManagerInfo noOpCacheManagerInfo() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return !context.cacheEnabled();
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return NoOpCacheManagerBuilder.build(context.cacheNames());
            }
        };
    }

    public CacheManagerInfo getCacheManagerInfoWithMicrometerMetrics() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && context.cacheType().equals(CAFFEINE_CACHE_TYPE)
                        && (context.metrics() == Context.Metrics.MICROMETER);
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return CaffeineCacheManagerBuilder.buildWithMicrometerMetrics(context.cacheNames(),
                        cacheConfigRV.getValue());
            }
        };
    }

    public CacheManagerInfo getCacheManagerInfoWithoutMetrics() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && context.cacheType().equals(CAFFEINE_CACHE_TYPE)
                        && (context.metrics() == Context.Metrics.NONE);
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return CaffeineCacheManagerBuilder.buildWithoutMetrics(context.cacheNames(), cacheConfigRV.getValue());
            }
        };
    }

}
