package io.quarkus.redis.deployment.client.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.codecs.Codec;
import io.quarkus.redis.datasource.codecs.Codecs;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class CustomCodecTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(Jedi.class).addClass(MyCustomCodec.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Test
    void testCustomCodecs() {
        String key1 = UUID.randomUUID().toString();
        // Check that the codec is registered
        assertThat(Codecs.getDefaultCodecFor(Jedi.class)).isInstanceOf(MyCustomCodec.class);

        HashCommands<String, String, Jedi> hash1 = ds.hash(Jedi.class);
        hash1.hset(key1, "test", new Jedi("luke", "skywalker"));
        var retrieved = hash1.hget(key1, "test");
        assertThat(retrieved.firstName).isEqualTo("luke");
        assertThat(retrieved.lastName).isEqualTo("SKYWALKER");

        HashCommands<String, Jedi, String> hash2 = ds.hash(String.class, Jedi.class, String.class);
        hash2.hset(key1, new Jedi("luke", "skywalker"), "test");
        var retrieved2 = hash2.hget(key1, new Jedi("luke", "skywalker"));
        assertThat(retrieved2).isEqualTo("test");

        HashCommands<Jedi, String, String> hash3 = ds.hash(Jedi.class, String.class, String.class);
        hash3.hset(new Jedi("luke", "skywalker"), "key", "value");
        var retrieved3 = hash3.hget(new Jedi("luke", "skywalker"), "key");
        assertThat(retrieved3).isEqualTo("value");
    }

    @ApplicationScoped
    public static class MyCustomCodec implements Codec {

        @Override
        public boolean canHandle(Type clazz) {
            return clazz.equals(Jedi.class);
        }

        @Override
        public byte[] encode(Object item) {
            var jedi = (Jedi) item;
            return (jedi.firstName + ";" + jedi.lastName).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Object decode(byte[] item) {
            String s = new String(item, StandardCharsets.UTF_8);
            String[] strings = s.split(";");
            return new Jedi(strings[0], strings[1].toUpperCase());
        }
    }

    public static class Jedi {
        public final String firstName;
        public final String lastName;

        public Jedi(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

}
