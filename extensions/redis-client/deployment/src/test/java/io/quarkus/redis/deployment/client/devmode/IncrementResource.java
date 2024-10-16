package io.quarkus.redis.deployment.client.devmode;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

@Path("/inc")
public class IncrementResource {

    public static final long INCREMENT = 1;
    private final ValueCommands<String, Integer> commands;
    private final KeyCommands<String> keys;

    public IncrementResource(RedisDataSource ds) {
        commands = ds.value(Integer.class);
        keys = ds.key();
    }

    @GET
    public int increment() {
        return (int) commands.incrby("counter-dev-mode", INCREMENT);
    }

    @GET
    @Path("/val")
    public int value() {
        return commands.get("counter-dev-mode");
    }

    @GET
    @Path("/keys")
    public int verifyPreloading() {
        return keys.keys("*").size();
    }

}
