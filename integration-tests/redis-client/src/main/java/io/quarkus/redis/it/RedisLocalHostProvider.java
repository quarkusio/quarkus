package io.quarkus.redis.it;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.redis.client.RedisHostsProvider;
import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
@Identifier("test-hosts-provider")
public class RedisLocalHostProvider implements RedisHostsProvider {

    // Select the database "3"
    @Override
    public Collection<URI> getHosts() {
        return List.of(URI.create("redis://localhost:6379/3"));
    }
}
