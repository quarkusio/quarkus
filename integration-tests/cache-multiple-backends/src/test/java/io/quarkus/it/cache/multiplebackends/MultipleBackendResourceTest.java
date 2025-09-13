package io.quarkus.it.cache.multiplebackends;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.cache.redis.runtime.RedisCacheImpl;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MultipleBackendResourceTest {
    private static final String DEFAULT_KEY_NAME = "my-key";

    @Test
    public void testCacheInstances() {
        // get the cache managers
        var cacheManagerMap = RestAssured
                .given()
                .when()
                .get("/cache-managers")
                .then()
                .statusCode(200)
                .extract()
                .as(Map.class);

        // assert redis cache is using the correct cache implementation
        Assertions.assertEquals(RedisCacheImpl.class.getName(),
                cacheManagerMap.get(MultipleBackendResource.REDIS_CACHE_NAME));

        // assert caffeine cache is using the correct cache implementation
        Assertions.assertEquals(CaffeineCacheImpl.class.getName(),
                cacheManagerMap.get(MultipleBackendResource.CAFFEINE_CACHE_NAME));
    }

    private String saveCacheValue(String backend) {
        return RestAssured
                .given()
                .when()
                .get("/caches/" + backend + "/" + DEFAULT_KEY_NAME)
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    private void invalidAllCache(String backend) {
        RestAssured
                .given()
                .when()
                .delete("/caches/" + backend)
                .then()
                .statusCode(204);
    }

    @Test
    public void testCacheStoredAndInvalidatedInCorrectCacheInstance() {
        // save the cache values in the cache backends
        String redisValue = saveCacheValue("redis");
        String caffeineValue = saveCacheValue("caffeine");

        // assert that the value for caffeine is stored in the caffeine cache
        Assertions.assertEquals(redisValue, getCacheValueFromCacheManagerForAssertion(
                MultipleBackendResource.REDIS_CACHE_NAME));
        // assert that the value for caffeine is stored in the caffeine cache
        Assertions.assertEquals(caffeineValue, getCacheValueFromCacheManagerForAssertion(
                MultipleBackendResource.CAFFEINE_CACHE_NAME));

        invalidAllCache("redis");
        Assertions.assertEquals("",
                getCacheValueFromCacheManagerForAssertion(MultipleBackendResource.REDIS_CACHE_NAME));
        Assertions.assertEquals(caffeineValue,
                getCacheValueFromCacheManagerForAssertion(MultipleBackendResource.CAFFEINE_CACHE_NAME));

        invalidAllCache("caffeine");
        Assertions.assertEquals("",
                getCacheValueFromCacheManagerForAssertion(MultipleBackendResource.REDIS_CACHE_NAME));
        Assertions.assertEquals("",
                getCacheValueFromCacheManagerForAssertion(MultipleBackendResource.CAFFEINE_CACHE_NAME));
    }

    private String getCacheValueFromCacheManagerForAssertion(String cacheName) {
        return RestAssured
                .given()
                .when()
                .get("/cache-managers/" + cacheName + "/keys/" + DEFAULT_KEY_NAME)
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }
}
