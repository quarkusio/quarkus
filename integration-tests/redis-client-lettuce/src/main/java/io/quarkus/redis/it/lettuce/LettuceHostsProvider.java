package io.quarkus.redis.it.lettuce;

import java.net.URI;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.redis.client.RedisHostsProvider;
import io.smallrye.common.annotation.Identifier;

/**
 * Provides the hosts of the {@code provided} client (see {@code application.properties}), configured through
 * {@code hosts-provider-name} instead of {@code hosts}: the standalone server, database 3, so that its connections
 * can be told from those of the default client.
 */
@ApplicationScoped
@Identifier("lettuce-it-hosts")
public class LettuceHostsProvider implements RedisHostsProvider {

    @Override
    public Set<URI> getHosts() {
        return Set.of(URI.create("redis://localhost:6379/3"));
    }
}
