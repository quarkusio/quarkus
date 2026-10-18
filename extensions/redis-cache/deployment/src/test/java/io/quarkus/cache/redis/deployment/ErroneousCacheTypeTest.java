package io.quarkus.cache.redis.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ErroneousCacheTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(SimpleCachedService.class))
            .overrideConfigKey("quarkus.cache.type", "not-redis")
            .setExpectedException(DeploymentException.class);
}
