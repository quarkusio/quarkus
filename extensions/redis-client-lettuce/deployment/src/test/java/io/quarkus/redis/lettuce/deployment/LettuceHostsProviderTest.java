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
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.smallrye.common.annotation.Identifier;
import io.vertx.redis.client.Command;

/**
 * Verifies that a Lettuce client configured through {@code hosts-provider-name}, with no {@code hosts}, connects to
 * the URI the {@link RedisHostsProvider} bean of that name returns, as the Vert.x client does: the data source and
 * the Lettuce beans of the client work, on the database of the provided URI.
 * <p>
 * Both backends call the provider from a {@code Phase.DATA} service that runs before the synthetic beans initialized
 * at runtime are usable, so it must not inject such beans. {@link LettuceHostsProviderInjectionTest} covers a
 * provider with dependencies, called by the Lettuce backend alone.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceHostsProviderTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(DatabaseTwoHostsProvider.class))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.redis.provided.hosts-provider-name", "database-two");

    @Inject
    @RedisClientName("provided")
    RedisDataSource ds;

    @Inject
    @RedisClientName("provided")
    StatefulRedisConnection<String, String> connection;

    @Test
    void dataSourceConnectsToTheProvidedHost() {
        assertThat(ds.execute(Command.PING).toString()).isEqualTo("PONG");
        assertThat(ds.execute(Command.CLIENT, "INFO").toString()).contains(" db=2 ");
    }

    @Test
    void lettuceBeansConnectToTheProvidedHost() {
        assertThat(connection.sync().clientInfo()).contains(" db=2 ");
    }

    /**
     * Provides the test server, database 2, so that the connections can be told from ones made through
     * {@code hosts}.
     */
    @ApplicationScoped
    @Identifier("database-two")
    public static class DatabaseTwoHostsProvider implements RedisHostsProvider {

        @ConfigProperty(name = "quarkus.redis.tr")
        String endpoint;

        @Override
        public Set<URI> getHosts() {
            return Set.of(URI.create(endpoint + "/2"));
        }
    }
}
