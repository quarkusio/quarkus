package io.quarkus.redis.it;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.quarkus.redis.client.RedisHostsProvider;

@ApplicationScoped
@Named("test-hosts-provider")
public class RedisLocalHostProvider implements RedisHostsProvider {
    @Override
    public Set<URI> getHosts() {
        return Collections.singleton(URI.create("redis://localhost:6379/3"));
    }
}
