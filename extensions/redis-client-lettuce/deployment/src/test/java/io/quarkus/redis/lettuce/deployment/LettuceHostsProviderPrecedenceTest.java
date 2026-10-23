package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.smallrye.common.annotation.Identifier;
import io.vertx.redis.client.Command;

/**
 * Verifies that {@code hosts} wins over {@code hosts-provider-name} when both are set, as with the Vert.x client: the
 * provider names a port nothing listens on, so the client works only if the provider is not consulted.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHostsProviderPrecedenceTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(UnreachableHostsProvider.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.hosts-provider-name", "unreachable");

    @Inject
    RedisDataSource ds;

    @Test
    void hostsWinOverTheProvider() {
        assertThat(ds.execute(Command.PING).toString()).isEqualTo("PONG");
    }

    @ApplicationScoped
    @Identifier("unreachable")
    public static class UnreachableHostsProvider implements RedisHostsProvider {

        @Override
        public Set<URI> getHosts() {
            return Set.of(URI.create("redis://localhost:1"));
        }
    }
}
