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

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.vertx.mutiny.redis.client.Redis;

/**
 * Verifies the readiness health check of an application using the Lettuce backend and the Vert.x Redis client side by
 * side: both clients are reported by the one health check, and the health check does not make the Vert.x Redis client
 * exist for the client used through its data source.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHealthCheckWithVertxClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxClientBeans.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.low-level.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("low-level")
    Redis lowLevel;

    @Test
    void bothClientsAreReported() {
        RestAssured.when().get("/q/health/ready").then()
                .statusCode(200)
                .body("status", is("UP"),
                        "checks.name", contains("Redis connection health check"),
                        "checks[0].data", aMapWithSize(2),
                        "checks[0].data", hasEntry("default", "PONG"),
                        "checks[0].data", hasEntry("low-level", "PONG"));
    }

    @Test
    void theVertxClientIsOnlyCreatedForTheClientInjectingIt() {
        assertThat(VertxClientBeans.clientNames()).containsExactly("low-level");
    }
}
