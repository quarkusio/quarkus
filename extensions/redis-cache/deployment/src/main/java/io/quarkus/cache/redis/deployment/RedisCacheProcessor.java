package io.quarkus.cache.redis.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.cache.deployment.CacheDeploymentConstants;
import io.quarkus.cache.deployment.CacheNamesBuildItem;
import io.quarkus.cache.deployment.spi.CacheManagerInfoBuildItem;
import io.quarkus.cache.redis.runtime.RedisCacheBuildRecorder;
import io.quarkus.cache.redis.runtime.RedisCacheBuildTimeConfig;
import io.quarkus.cache.redis.runtime.RedisCachesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.smallrye.mutiny.Uni;

public class RedisCacheProcessor {

    private static final Logger LOGGER = Logger.getLogger(RedisCacheProcessor.class);

    public static final DotName UNI = DotName.createSimple(Uni.class.getName());

    @BuildStep
    @Record(RUNTIME_INIT)
    CacheManagerInfoBuildItem cacheManagerInfo(RedisCacheBuildRecorder recorder) {
        return new CacheManagerInfoBuildItem(recorder.getCacheManagerSupplier());
    }

    @BuildStep
    UnremovableBeanBuildItem redisClientUnremoveable() {
        return UnremovableBeanBuildItem.beanTypes(io.vertx.redis.client.Redis.class, io.vertx.mutiny.redis.client.Redis.class);
    }

    @BuildStep
    RequestedRedisClientBuildItem requestedRedisClientBuildItem(RedisCachesBuildTimeConfig buildConfig) {
        return new RequestedRedisClientBuildItem(buildConfig.clientName.orElse(RedisConfig.DEFAULT_CLIENT_NAME));
    }

    @BuildStep
    void nativeImage(BuildProducer<ReflectiveClassBuildItem> producer) {
        producer.produce(ReflectiveClassBuildItem.builder(CompositeCacheKey.class).methods(true).build());
    }

    @BuildStep
    @Record(STATIC_INIT)
    void determineValueTypes(RedisCacheBuildRecorder recorder, CombinedIndexBuildItem combinedIndex,
            CacheNamesBuildItem cacheNamesBuildItem, RedisCachesBuildTimeConfig buildConfig) {
        Map<String, String> resolvedValuesTypesFromAnnotations = valueTypesFromCacheResultAnnotation(combinedIndex);

        Map<String, String> valueTypes = new HashMap<>();
        Optional<String> defaultValueType = buildConfig.defaultConfig.valueType;
        Set<String> cacheNames = cacheNamesBuildItem.getNames();
        for (String cacheName : cacheNames) {
            String valueType = null;
            RedisCacheBuildTimeConfig cacheSpecificGroup = buildConfig.cachesConfig.get(cacheName);
            if (cacheSpecificGroup == null) {
                if (defaultValueType.isPresent()) {
                    valueType = defaultValueType.get();
                }
            } else {
                if (cacheSpecificGroup.valueType.isPresent()) {
                    valueType = cacheSpecificGroup.valueType.get();
                }
            }

            if (valueType == null) { // TODO: does it make sense to use the return type of method annotated with @CacheResult as the last resort or should it override the default cache config?
                valueType = resolvedValuesTypesFromAnnotations.get(cacheName);
            }

            if (valueType != null) {
                valueTypes.put(cacheName, valueType);
            } else {
                throw new DeploymentException("Unable to determine the value type for '" + cacheName
                        + "' Redis cache. An appropriate configuration value for 'quarkus.cache.redis." + cacheName
                        + ".value-type' needs to be set");
            }
        }
        recorder.setCacheValueTypes(valueTypes);
    }

    private static Map<String, String> valueTypesFromCacheResultAnnotation(CombinedIndexBuildItem combinedIndex) {
        Map<String, Set<Type>> valueTypesFromAnnotations = new HashMap<>();

        // first go through @CacheResult instances and simply record the return types
        for (AnnotationInstance instance : combinedIndex.getIndex().getAnnotations(CacheDeploymentConstants.CACHE_RESULT)) {
            if (instance.target().kind() != METHOD) {
                continue;
            }
            Type methodReturnType = instance.target().asMethod().returnType();
            if (methodReturnType.kind() == Type.Kind.VOID) {
                continue;
            }
            AnnotationValue cacheNameValue = instance.value("cacheName");
            if (cacheNameValue == null) {
                continue;
            }
            String cacheName = cacheNameValue.asString();
            Set<Type> types = valueTypesFromAnnotations.get(cacheName);
            if (types == null) {
                types = new HashSet<>(1);
                valueTypesFromAnnotations.put(cacheName, types);
            }
            types.add(methodReturnType);
        }

        if (valueTypesFromAnnotations.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        // now apply our resolution logic on the obtained types
        for (var entry : valueTypesFromAnnotations.entrySet()) {
            String cacheName = entry.getKey();
            Set<Type> typeSet = entry.getValue();
            if (typeSet.size() != 1) {
                LOGGER.debugv("Cache named '{0}' is used on methods with different result types", cacheName);
                // TODO: when there are multiple types for the same @CacheResult, should we fail? Should we try and be smarter in determining the type?
                continue;
            }

            Type type = typeSet.iterator().next();
            String resolvedType = null;
            if (type.kind() == Type.Kind.CLASS) {
                resolvedType = type.asClassType().name().toString();
            } else if (type.kind() == Type.Kind.PRIMITIVE) {
                resolvedType = type.asPrimitiveType().name().toString();
            } else if ((type.kind() == Type.Kind.PARAMETERIZED_TYPE) && UNI.equals(type.name())) {
                ParameterizedType parameterizedType = type.asParameterizedType();
                List<Type> arguments = parameterizedType.arguments();
                if (arguments.size() == 1) {
                    resolvedType = arguments.get(0).name().toString();
                }
            }

            if (resolvedType != null) {
                result.put(cacheName, resolvedType);
            } else {
                LOGGER.debugv(
                        "Cache named '{0}' is used on method whose return type '{1}' is not eligible for automatic resolution",
                        cacheName, type);
            }
        }

        return result;
    }
}
