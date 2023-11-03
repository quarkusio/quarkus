package io.quarkus.redis.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;

@Path("/quarkus-redis-with-instance")
@ApplicationScoped
public class RedisWithInstanceInjectionResource {

    private final ValueCommands<String, String> blocking;
    private final ReactiveValueCommands<String, String> reactive;

    public RedisWithInstanceInjectionResource(@Any Instance<RedisDataSource> ds,
            @Any Instance<ReactiveRedisDataSource> reactiveDs) {
        blocking = ds.select(RedisClientName.Literal.of("instance-client")).get().value(String.class);
        reactive = reactiveDs.select(RedisClientName.Literal.of("instance-client")).get().value(String.class);
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

}
