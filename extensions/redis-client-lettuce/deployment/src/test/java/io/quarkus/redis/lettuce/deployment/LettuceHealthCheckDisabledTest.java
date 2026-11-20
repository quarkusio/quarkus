package io.quarkus.redis.lettuce.deployment;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

/**
 * Verifies that {@code quarkus.redis.health.enabled=false} removes the Redis health check with the Lettuce backend,
 * as it does with the Vert.x client.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHealthCheckDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.health.enabled", "false");

    @Inject
    RedisDataSource ds;

    @Test
    void thereIsNoRedisHealthCheck() {
        RestAssured.when().get("/q/health/ready").then()
                .statusCode(200)
                .body("status", is("UP"),
                        "checks", empty());
    }
}
