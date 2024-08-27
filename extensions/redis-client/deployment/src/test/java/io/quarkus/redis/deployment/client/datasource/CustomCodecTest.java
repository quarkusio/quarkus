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
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(Jedi.class).addClass(Sith.class)
                            .addClass(CustomJediCodec.class).addClass(CustomSithCodec.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Test
    void testCustomCodecs() {
        String key1 = UUID.randomUUID().toString();
        // Check that all codec are registered
        assertThat(Codecs.getDefaultCodecFor(Jedi.class)).isInstanceOf(CustomJediCodec.class);
        assertThat(Codecs.getDefaultCodecFor(Sith.class)).isInstanceOf(CustomSithCodec.class);

        HashCommands<String, String, Jedi> jediHash1 = ds.hash(Jedi.class);
        jediHash1.hset(key1, "test", new Jedi("luke", "skywalker"));
        var jediRetrieved = jediHash1.hget(key1, "test");
        assertThat(jediRetrieved.firstName).isEqualTo("luke");
        assertThat(jediRetrieved.lastName).isEqualTo("SKYWALKER");

        HashCommands<String, Jedi, String> jediHash2 = ds.hash(String.class, Jedi.class, String.class);
        jediHash2.hset(key1, new Jedi("luke", "skywalker"), "test");
        var jediRetrieved2 = jediHash2.hget(key1, new Jedi("luke", "skywalker"));
        assertThat(jediRetrieved2).isEqualTo("test");

        HashCommands<Jedi, String, String> jediHash3 = ds.hash(Jedi.class, String.class, String.class);
        jediHash3.hset(new Jedi("luke", "skywalker"), "key", "value");
        var jediRetrieved3 = jediHash3.hget(new Jedi("luke", "skywalker"), "key");
        assertThat(jediRetrieved3).isEqualTo("value");

        HashCommands<String, String, Sith> sithHash1 = ds.hash(Sith.class);
        sithHash1.hset(key1, "test", new Sith("darth", "sidious"));
        var sithRetrieved = sithHash1.hget(key1, "test");
        assertThat(sithRetrieved.firstName).isEqualTo("darth");
        assertThat(sithRetrieved.lastName).isEqualTo("SIDIOUS");

        HashCommands<String, Sith, String> sithHash2 = ds.hash(String.class, Sith.class, String.class);
        sithHash2.hset(key1, new Sith("darth", "sidious"), "test");
        var sithRetrieved2 = sithHash2.hget(key1, new Sith("darth", "sidious"));
        assertThat(sithRetrieved2).isEqualTo("test");

        HashCommands<Sith, String, String> sithHash3 = ds.hash(Sith.class, String.class, String.class);
        sithHash3.hset(new Sith("darth", "sidious"), "key", "value");
        var sithRetrieved3 = sithHash3.hget(new Sith("darth", "sidious"), "key");
        assertThat(sithRetrieved3).isEqualTo("value");
    }

    @ApplicationScoped
    public static class CustomJediCodec implements Codec {

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

    @ApplicationScoped
    public static class CustomSithCodec implements Codec {

        @Override
        public boolean canHandle(Type clazz) {
            return clazz.equals(Sith.class);
        }

        @Override
        public byte[] encode(Object item) {
            var sith = (Sith) item;
            return (sith.firstName + ";" + sith.lastName).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Object decode(byte[] item) {
            String s = new String(item, StandardCharsets.UTF_8);
            String[] strings = s.split(";");
            return new Sith(strings[0], strings[1].toUpperCase());
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

    public static class Sith {
        public final String firstName;
        public final String lastName;

        public Sith(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

}
