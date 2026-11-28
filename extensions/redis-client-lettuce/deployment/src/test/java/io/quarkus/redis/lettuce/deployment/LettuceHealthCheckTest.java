package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

/**
 * Verifies the readiness health check with the Lettuce backend: the clients used through their data sources are
 * pinged through Lettuce, an inactive one is left out, and the health check does not make the Vert.x Redis client
 * exist for them.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHealthCheckTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxClientBeans.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.named.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.inactive.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.inactive.active", "false");

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("named")
    ReactiveRedisDataSource named;

    @Inject
    @RedisClientName("inactive")
    InjectableInstance<RedisDataSource> inactive;

    @Test
    void theDataSourcesArePingedThroughLettuce() {
        RestAssured.when().get("/q/health/ready").then()
                .statusCode(200)
                .body("status", is("UP"),
                        "checks.name", contains("Redis connection health check"),
                        "checks.status", contains("UP"),
                        "checks[0].data", aMapWithSize(2),
                        "checks[0].data", hasEntry("default", "PONG"),
                        "checks[0].data", hasEntry("named", "PONG"));
    }

    @Test
    void noVertxClientIsCreated() {
        assertThat(VertxClientBeans.clientNames()).isEmpty();
    }
}
