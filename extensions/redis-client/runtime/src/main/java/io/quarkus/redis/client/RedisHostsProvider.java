package io.quarkus.redis.client;

import java.net.URI;
import java.util.Collection;

/**
 * Programmatically provides redis hosts
 */
public interface RedisHostsProvider {
    /**
     * Returns the hosts for this provider.
     * <p>
     * The host provided uses the following schema `redis://[username:password@][host][:port][/database]`
     *
     * @return the hosts
     */
    Collection<URI> getHosts();
}
