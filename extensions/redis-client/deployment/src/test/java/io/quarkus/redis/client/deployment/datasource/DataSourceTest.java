package io.quarkus.redis.client.deployment.datasource;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.deployment.RedisTestResource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class DataSourceTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis.r1.hosts", "${quarkus.redis.tr}/1")
            .overrideConfigKey("quarkus.redis.r2.hosts", "${quarkus.redis.tr}/2");

    @Inject
    @RedisClientName("r1")
    RedisDataSource r1;

    @Inject
    @RedisClientName("r2")
    RedisDataSource r2;

    @Test
    public void testThatTheDatasourceUseDifferentDatabases() {
        ValueCommands<String, String> r1s = r1.value(String.class);
        ValueCommands<String, String> r2s = r2.value(String.class);

        r1s.set("key", "hello");
        Assertions.assertThat(r2s.get("key")).isNull();
        Assertions.assertThat(r1s.get("key")).isEqualTo("hello");
    }

}
