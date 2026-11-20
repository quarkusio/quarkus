package io.quarkus.redis.lettuce.deployment;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

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

/**
 * Verifies that the readiness health check reports a client of the Lettuce backend as down when its server cannot be
 * reached, next to one whose server answers.
 */
@QuarkusTestResource(FreePortTestResource.class)
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHealthCheckDownTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.away.hosts", "redis://127.0.0.1:${lettuce.test.free-port}");

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("away")
    RedisDataSource away;

    @Test
    void theClientIsReportedDown() {
        RestAssured.when().get("/q/health/ready").then()
                .statusCode(503)
                .body("status", is("DOWN"),
                        "checks.name", contains("Redis connection health check"),
                        "checks.status", contains("DOWN"),
                        "checks[0].data", hasEntry("default", "PONG"),
                        "checks[0].data.away",
                        startsWith("Unable to execute the validation check for the Redis Client"));
    }
}
