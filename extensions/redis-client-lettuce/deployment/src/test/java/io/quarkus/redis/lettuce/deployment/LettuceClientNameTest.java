package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that the Lettuce backend names its connections as the Vert.x client does: with {@code client-name} when
 * {@code configure-client-name} is set, with the {@code client} query parameter of a host URI, and not with
 * {@code client-name} alone; and that RESP3 is negotiated by default.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceClientNameTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.configure-client-name", "true")
            .overrideConfigKey("quarkus.redis.client-name", "lettuce-test-app")
            .overrideConfigKey("quarkus.redis.from-uri.hosts", "${quarkus.redis.tr}?client=named-by-uri")
            .overrideConfigKey("quarkus.redis.unnamed.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.unnamed.client-name", "not-applied-without-configure-client-name");

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("from-uri")
    RedisDataSource fromUri;

    @Inject
    @RedisClientName("unnamed")
    RedisDataSource unnamed;

    @Test
    void namesTheConnections() {
        assertThat(connection.sync().clientGetname()).isEqualTo("lettuce-test-app");
        assertThat(ds.execute("CLIENT", "INFO").toString()).contains(" name=lettuce-test-app ");
        assertThat(fromUri.execute("CLIENT", "INFO").toString()).contains(" name=named-by-uri ");
        // client-name alone is not applied, as with the Vert.x client
        assertThat(unnamed.execute("CLIENT", "INFO").toString()).contains(" name= ");
    }

    @Test
    void negotiatesResp3ByDefault() {
        assertThat(connection.sync().clientInfo()).contains(" resp=3 ");
        assertThat(ds.execute("CLIENT", "INFO").toString()).contains(" resp=3 ");
    }
}
