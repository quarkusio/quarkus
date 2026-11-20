package io.quarkus.redis.it.lettuce;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.arc.Arc;
import io.vertx.mutiny.redis.client.Redis;

/**
 * Reports the Vert.x Redis clients of the application. Every client of this module is used through its data sources
 * or through the Lettuce types, so there must be none, with the health check enabled too.
 */
@Path("/lettuce/vertx-clients")
@ApplicationScoped
public class LettuceVertxClientResource {

    @GET
    public int count() {
        return Arc.container().listAll(Redis.class, Any.Literal.INSTANCE).size();
    }

}
