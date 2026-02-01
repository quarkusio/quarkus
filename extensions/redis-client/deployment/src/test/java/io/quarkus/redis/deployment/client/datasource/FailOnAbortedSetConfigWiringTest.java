package io.quarkus.redis.deployment.client.datasource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisCommandAbortedException;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class FailOnAbortedSetConfigWiringTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyRedisApp.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.fail-on-aborted-set", "true");

    @Inject
    MyRedisApp app;

    @Test
    public void testFailOnAbortedSetIsWiredFromConfig() {
        String key = "fail-on-aborted-set-wiring-" + UUID.randomUUID();
        app.del(key);

        app.setNx(key, "v1");

        assertThatThrownBy(() -> app.setNx(key, "v2"))
                .isInstanceOf(RedisCommandAbortedException.class);
    }

    @ApplicationScoped
    public static class MyRedisApp {

        @Inject
        ReactiveRedisDataSource reactive;

        void del(String key) {
            reactive.key(String.class).del(key)
                    .await().indefinitely();
        }

        void setNx(String key, String value) {
            reactive.value(String.class)
                    .set(key, value, new SetArgs().nx())
                    .await().indefinitely();
        }
    }
}
