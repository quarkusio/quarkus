package io.quarkus.cache.redis.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.quarkus.arc.Arc;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class PojoAndMultipleKeysCacheTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(CachedService.class, Message.class, TestUtil.class));

    @Inject
    CachedService cachedService;

    @Test
    public void test() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        Message messageFromKey1 = cachedService.getMessage("hello", "a", "1").await().indefinitely();
        List<String> newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());

        assertEquals(messageFromKey1, cachedService.getMessage("hello", "b", "1").await().indefinitely());
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());

        Message messageFromKey2 = cachedService.getMessage("world", "c", "1").await().indefinitely();
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 2, newKeys.size());

        assertEquals(messageFromKey2, cachedService.getMessage("world", "d", "1").await().indefinitely());
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 2, newKeys.size());

        Message otherMessageFromKey1 = cachedService.getMessage("hello", "e", "2").await().indefinitely();
        assertNotEquals(otherMessageFromKey1, messageFromKey1);
        assertEquals(otherMessageFromKey1, cachedService.getMessage("hello", "f", "2").await().indefinitely());
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 3, newKeys.size());
    }

    @Singleton
    public static class CachedService {

        @CacheResult(cacheName = "message")
        public Uni<Message> getMessage(@CacheKey String key, String notPartOfTheKey, @CacheKey String otherKey) {
            return Uni.createFrom()
                    .item(new Message(UUID.randomUUID().toString(), ThreadLocalRandom.current().nextInt()));
        }
    }

    public static class Message {
        private final String str;
        private final int num;

        @JsonCreator
        public Message(String str, int num) {
            this.str = str;
            this.num = num;
        }

        public String getStr() {
            return str;
        }

        public int getNum() {
            return num;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Message message = (Message) o;
            return num == message.num && str.equals(message.str);
        }

        @Override
        public int hashCode() {
            return Objects.hash(str, num);
        }
    }
}
