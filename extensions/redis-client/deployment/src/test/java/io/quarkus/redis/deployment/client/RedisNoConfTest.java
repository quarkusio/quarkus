package io.quarkus.redis.deployment.client;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.redis.client.Redis;

public class RedisNoConfTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false");

    @Inject
    @Any
    Instance<Redis> redis;
    @Inject
    @Any
    Instance<RedisAPI> apis;

    @Test
    public void nothingShouldBeExposed() {
        Assertions.assertTrue(redis.isUnsatisfied());
        Assertions.assertTrue(apis.isUnsatisfied());
    }
}
