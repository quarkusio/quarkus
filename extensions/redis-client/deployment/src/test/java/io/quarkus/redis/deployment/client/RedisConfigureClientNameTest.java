package io.quarkus.redis.deployment.client;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.Command;

@QuarkusTestResource(RedisTestResource.class)
public class RedisConfigureClientNameTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis.my-redis.hosts", "${quarkus.redis.tr}/1")
            .overrideConfigKey("quarkus.redis.my-redis.configure-client-name", "true")
            .overrideConfigKey("quarkus.redis.my-redis.client-name", "perfect-client-name")
            .overrideConfigKey("quarkus.redis.no-configure.hosts", "${quarkus.redis.tr}/2")
            .overrideConfigKey("quarkus.redis.no-configure.client-name", "i-am-not-applicable")
            .overrideConfigKey("quarkus.redis.no-configure.configure-client-name", "false")
            .overrideConfigKey("quarkus.redis.from-annotation.configure-client-name", "true")
            .overrideConfigKey("quarkus.redis.from-annotation.hosts", "${quarkus.redis.tr}/3");

    @Inject
    @RedisClientName("my-redis")
    RedisDataSource myRedis;

    @Inject
    @RedisClientName("no-configure")
    RedisDataSource noConfigure;

    @Inject
    @RedisClientName("from-annotation")
    RedisDataSource fromAnnotation;

    @Test
    void shouldConfigureClientNameCorrectly() {
        Response executed = myRedis.execute(Command.CLIENT, "GETNAME");
        Assertions.assertThat(executed).isNotNull();
        Assertions.assertThat(executed.toString()).isEqualTo("perfect-client-name");
    }

    @Test
    void shouldConfigureFromRedisClientNameAnnotation() {
        Response executed = fromAnnotation.execute(Command.CLIENT, "GETNAME");
        Assertions.assertThat(executed).isNotNull();
        Assertions.assertThat(executed.toString()).isEqualTo("from-annotation");
    }

    @Test
    void shouldNotConfigureClientName() {
        Response executed = noConfigure.execute(Command.CLIENT, "GETNAME");
        Assertions.assertThat(executed).isNull();
    }
}
