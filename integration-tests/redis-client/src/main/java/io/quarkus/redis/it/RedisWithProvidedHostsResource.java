package io.quarkus.redis.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.string.StringCommands;
import io.smallrye.mutiny.Uni;

@Path("/quarkus-redis-provided-hosts")
@ApplicationScoped
public class RedisWithProvidedHostsResource {

    private final StringCommands<String, String> blocking;
    private final ReactiveStringCommands<String, String> reactive;

    @Inject
    public RedisWithProvidedHostsResource(@RedisClientName("provided-hosts") RedisDataSource ds,
            @RedisClientName("provided-hosts") ReactiveRedisDataSource reactiveDs) {
        blocking = ds.string(String.class);
        reactive = reactiveDs.string(String.class);
    }

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
