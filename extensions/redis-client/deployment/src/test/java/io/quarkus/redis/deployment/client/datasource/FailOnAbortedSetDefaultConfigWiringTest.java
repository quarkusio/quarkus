package io.quarkus.redis.deployment.client.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class FailOnAbortedSetDefaultConfigWiringTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyRedisApp.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    MyRedisApp app;

    @Test
    public void testFailOnAbortedSetDefaultsToFalse() {
        String key = "fail-on-aborted-set-default-" + UUID.randomUUID();
        app.del(key);

        app.setNx(key, "v1");

        assertThatCode(() -> app.setNx(key, "v2"))
                .doesNotThrowAnyException();

        assertThat(app.get(key)).isEqualTo("v1");
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

        String get(String key) {
            return reactive.value(String.class).get(key)
                    .await().indefinitely();
        }
    }
}
