package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code preferred-protocol-version=resp2} and {@code protocol-negotiation=false} make the Lettuce
 * backend speak RESP2, as they do with the Vert.x client, and that the data source works over it.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceProtocolVersionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.preferred-protocol-version", "resp2")
            .overrideConfigKey("quarkus.redis.legacy.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.legacy.protocol-negotiation", "false");

    @Inject
    RedisDataSource preferred;

    @Inject
    @RedisClientName("legacy")
    RedisDataSource legacy;

    @Test
    void speaksResp2() {
        assertThat(preferred.execute("CLIENT", "INFO").toString()).contains(" resp=2 ");
        assertThat(legacy.execute("CLIENT", "INFO").toString()).contains(" resp=2 ");
    }

    @Test
    void dataSourceWorksOverResp2() {
        preferred.value(String.class).set("lettuce:resp2:key", "value");
        assertThat(preferred.value(String.class).get("lettuce:resp2:key")).isEqualTo("value");
        assertThat(preferred.hash(String.class).hgetall("lettuce:resp2:missing")).isEmpty();
        legacy.value(String.class).set("lettuce:resp2:legacy", "value");
        assertThat(legacy.value(String.class).get("lettuce:resp2:legacy")).isEqualTo("value");
    }
}
