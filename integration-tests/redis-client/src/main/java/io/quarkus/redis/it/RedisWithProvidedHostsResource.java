package io.quarkus.redis.it;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Uni;
import io.vertx.redis.client.Response;

@Path("/quarkus-redis-provided-hosts")
@ApplicationScoped
public class RedisWithProvidedHostsResource {
    private RedisClient redisClient;
    private ReactiveRedisClient reactiveRedisClient;

    @Inject
    public RedisWithProvidedHostsResource(@RedisClientName("provided-hosts") RedisClient redisClient,
            @RedisClientName("provided-hosts") ReactiveRedisClient reactiveRedisClient) {
        this.redisClient = redisClient;
        this.reactiveRedisClient = reactiveRedisClient;
    }

    // synchronous
    @GET
    @Path("/sync/{key}")
    public String getSync(@PathParam("key") String key) {
        Response response = redisClient.get(key);
        return response == null ? null : response.toString();
    }

    @POST
    @Path("/sync/{key}")
    public void setSync(@PathParam("key") String key, String value) {
        this.redisClient.set(Arrays.asList(key, value));
    }

    // reactive
    @GET
    @Path("/reactive/{key}")
    public Uni<String> getReactive(@PathParam("key") String key) {
        return reactiveRedisClient
                .get(key)
                .map(response -> response == null ? null : response.toString());
    }

    @POST
    @Path("/reactive/{key}")
    public Uni<Void> setReactive(@PathParam("key") String key, String value) {
        return this.reactiveRedisClient
                .set(Arrays.asList(key, value))
                .map(response -> null);
    }

}
