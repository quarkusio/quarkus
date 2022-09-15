package io.quarkus.redis.client.deployment.patterns;

import java.util.NoSuchElementException;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.deployment.RedisTestResource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class BinaryTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyBinaryRepository.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    MyBinaryRepository repo;

    @Test
    void binary() {
        Random random = new Random();
        byte[] pic1 = new byte[1024];
        byte[] pic2 = new byte[1024];
        random.nextBytes(pic1);
        random.nextBytes(pic2);

        Assertions.assertThrows(NoSuchElementException.class, () -> repo.get("binary-foo"));

        repo.add("binary-foo", pic1);
        repo.addIfAbsent("binary-bar", pic2);
        repo.addIfAbsent("binary-bar", pic1);

        byte[] p1 = repo.get("binary-foo");
        byte[] p2 = repo.get("binary-bar");

        Assertions.assertArrayEquals(p1, pic1);
        Assertions.assertArrayEquals(p2, pic2);
    }

    @ApplicationScoped
    public static class MyBinaryRepository {

        private final ValueCommands<String, byte[]> commands;

        public MyBinaryRepository(RedisDataSource ds) {
            commands = ds.value(byte[].class);
        }

        public byte[] get(String key) {
            byte[] bytes = commands.get(key);
            if (bytes == null) {
                throw new NoSuchElementException("`" + key + "` not found");
            }
            return bytes;
        }

        public void add(String key, byte[] bytes) {
            commands.set(key, bytes);
        }

        public void addIfAbsent(String key, byte[] bytes) {
            commands.setnx(key, bytes);
        }
    }

}
