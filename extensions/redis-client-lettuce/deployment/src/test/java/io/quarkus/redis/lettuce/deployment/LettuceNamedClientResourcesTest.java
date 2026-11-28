package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.arc.ClientProxy;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class LettuceNamedClientResourcesTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.with-delay.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.with-delay.reconnect-interval", "2s")
            .overrideConfigKey("quarkus.redis.plain.hosts", "${quarkus.redis.tr}");

    @Inject
    ClientResources sharedResources;

    @Inject
    RedisClient defaultClient;

    @Inject
    @RedisClientName("with-delay")
    ClientResources withDelayResources;

    @Inject
    @RedisClientName("with-delay")
    RedisClient withDelayClient;

    @Inject
    @RedisClientName("plain")
    ClientResources plainResources;

    @Inject
    @RedisClientName("plain")
    RedisClient plainClient;

    @Test
    public void namedBeanIsTheResourcesOfItsClient() {
        assertThat(ClientProxy.unwrap(withDelayResources)).isSameAs(withDelayClient.getResources());
        assertThat(ClientProxy.unwrap(plainResources)).isSameAs(plainClient.getResources());
    }

    @Test
    public void clientWithReconnectDelayHasItsOwnResourcesOnTheSharedEventLoops() {
        ClientResources shared = ClientProxy.unwrap(sharedResources);
        ClientResources own = ClientProxy.unwrap(withDelayResources);
        assertThat(own).isNotSameAs(shared);
        assertThat(own.eventLoopGroupProvider()).isSameAs(shared.eventLoopGroupProvider());
        assertThat(own.timer()).isSameAs(shared.timer());
        assertThat(own.reconnectDelay()).isNotSameAs(shared.reconnectDelay());
    }

    @Test
    public void clientWithoutResourceSettingsUsesTheSharedResources() {
        ClientResources shared = ClientProxy.unwrap(sharedResources);
        assertThat(ClientProxy.unwrap(plainResources)).isSameAs(shared);
        assertThat(defaultClient.getResources()).isSameAs(shared);
    }
}
