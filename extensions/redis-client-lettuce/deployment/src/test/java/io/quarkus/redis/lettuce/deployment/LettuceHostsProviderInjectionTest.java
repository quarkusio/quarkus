package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.smallrye.common.annotation.Identifier;

/**
 * Verifies that the Lettuce backend calls the {@link RedisHostsProvider} bean of a client once the CDI container is
 * running: the provider injects configuration and another application bean, as a provider discovering the hosts
 * would.
 * <p>
 * The default client is requested through a Lettuce bean only, and no {@code @RedisClientName} annotation names
 * another one, so no Vert.x Redis client is created (see {@link LettuceNoVertxBeansTest}). Like the Vert.x client,
 * the Lettuce backend calls the provider from a {@code Phase.DATA} service, before the synthetic beans initialized at
 * runtime (the Vert.x instance, the data sources) are usable, so a provider must not inject such beans (see
 * {@link LettuceHostsProviderTest}).
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHostsProviderInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(DatabaseTwoHostsProvider.class, DatabaseSelector.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.hosts-provider-name", "database-two");

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Test
    void lettuceBeansConnectToTheProvidedHost() {
        assertThat(connection.sync().clientInfo()).contains(" db=2 ");
    }

    /**
     * Provides the test server, on the database {@link DatabaseSelector} picks.
     */
    @ApplicationScoped
    @Identifier("database-two")
    public static class DatabaseTwoHostsProvider implements RedisHostsProvider {

        @ConfigProperty(name = "quarkus.redis.tr")
        String endpoint;

        @Inject
        DatabaseSelector selector;

        @Override
        public Set<URI> getHosts() {
            assertThat(selector).isNotNull();
            return Set.of(URI.create(endpoint + "/" + selector.database()));
        }
    }

    @ApplicationScoped
    public static class DatabaseSelector {

        int database() {
            return 2;
        }
    }
}
