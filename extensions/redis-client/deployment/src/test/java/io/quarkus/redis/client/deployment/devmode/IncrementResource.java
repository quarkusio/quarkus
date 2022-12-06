package io.quarkus.redis.client.deployment.devmode;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;

@Path("/inc")
public class IncrementResource {

    public static final long INCREMENT = 1;
    private final ValueCommands<String, Integer> commands;

    public IncrementResource(RedisDataSource ds) {
        commands = ds.value(Integer.class);
    }

    @GET
    public int increment() {
        return (int) commands.incrby("counter-dev-mode", INCREMENT);
    }

}
