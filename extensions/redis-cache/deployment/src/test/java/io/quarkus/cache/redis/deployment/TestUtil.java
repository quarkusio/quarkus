package io.quarkus.cache.redis.deployment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;

final class TestUtil {

    private TestUtil() {
    }

    static List<String> allRedisKeys(RedisDataSource redisDataSource) {
        Iterator<String> iter = redisDataSource.key().scan().toIterable().iterator();
        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }
}
