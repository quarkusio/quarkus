package io.quarkus.redis.it.lettuce;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

/**
 * Uses the Lettuce connection bean of the default client: through Lettuce's own synchronous API, and by requesting
 * its reactive API, which needs Project Reactor. This module has no Reactor on its class path (the enforcer rule of
 * the pom bans it), as an application using the Lettuce backend has none by default.
 */
@Path("/lettuce/connection")
@ApplicationScoped
public class LettuceConnectionResource {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    public LettuceConnectionResource(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    @GET
    @Path("/ping")
    public String ping() {
        return connection.sync().ping();
    }

    /**
     * The message of the failure the reactive API is refused with. In a native image that is the
     * {@code UnsupportedOperationException} of the substituted factory; in JVM mode the request fails with a
     * {@code NoClassDefFoundError} instead, which is not caught here.
     */
    @GET
    @Path("/reactive-api")
    public String reactiveApi() {
        try {
            connection.commands(RedisReactiveCommands.factory());
        } catch (UnsupportedOperationException e) {
            return e.getMessage();
        }
        return "the reactive API is available";
    }

}
