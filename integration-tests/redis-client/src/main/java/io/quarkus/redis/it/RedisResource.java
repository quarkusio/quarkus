package io.quarkus.redis.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;

@Path("/quarkus-redis")
@ApplicationScoped
public class RedisResource {

    private final ValueCommands<String, String> blocking;
    private final ReactiveValueCommands<String, String> reactive;
    private final ReactiveKeyCommands<String> keys;

    public RedisResource(RedisDataSource ds,
            ReactiveRedisDataSource reactiveDs) {
        blocking = ds.value(String.class);
        reactive = reactiveDs.value(String.class);
        keys = reactiveDs.key();
    }

    // synchronous
    @GET
    @Path("/sync/{key}")
    public String getSync(@PathParam("key") String key) {
        return blocking.get(key);
    }

    @POST
    @Path("/sync/{key}")
    public void setSync(@PathParam("key") String key, String value) {
        blocking.set(key, value);
    }

    // reactive
    @GET
    @Path("/reactive/{key}")
    public Uni<String> getReactive(@PathParam("key") String key) {
        return reactive.get(key);
    }

    @POST
    @Path("/reactive/{key}")
    public Uni<Void> setReactive(@PathParam("key") String key, String value) {
        return this.reactive.set(key, value);
    }

    @GET
    @Path("/import")
    public Uni<Integer> startWarsKey() {
        return keys.keys("people:*").map(List::size);
    }

}
