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
import io.vertx.core.Vertx;

/**
 * Verifies that the Lettuce backend only calls the {@link RedisHostsProvider} bean of a client once the synthetic
 * beans initialized at runtime are usable: the provider injects the Vert.x instance, as a provider discovering the
 * hosts would.
 * <p>
 * The default client is requested through a Lettuce bean only, and no {@code @RedisClientName} annotation names
 * another one, so no Vert.x Redis client is created (see {@link LettuceNoVertxBeansTest}): the Vert.x client calls
 * the provider from a {@code Phase.DATA} service, before those beans are initialized (see
 * {@link LettuceHostsProviderTest}).
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHostsProviderInitOrderTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(DatabaseTwoHostsProvider.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.hosts-provider-name", "database-two");

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Test
    void lettuceBeansConnectToTheProvidedHost() {
        assertThat(connection.sync().clientInfo()).contains(" db=2 ");
    }

    /**
     * Provides the test server, database 2. Injects the Vert.x instance, a bean initialized at runtime like a data
     * source or a Vert.x Redis client: the backend must only call the provider once such beans are usable.
     */
    @ApplicationScoped
    @Identifier("database-two")
    public static class DatabaseTwoHostsProvider implements RedisHostsProvider {

        @ConfigProperty(name = "quarkus.redis.tr")
        String endpoint;

        @Inject
        Vertx vertx;

        @Override
        public Set<URI> getHosts() {
            assertThat(vertx).isNotNull();
            return Set.of(URI.create(endpoint + "/2"));
        }
    }
}
